param(
    [string]$BaseUrl = "http://127.0.0.1:8849",
    [string]$Database = "pxczxn_community",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "MMddHHmmssfff"
$usernameA = "m2ca_$stamp"
$usernameB = "m2cb_$stamp"
$emailA = "$usernameA@example.test"
$emailB = "$usernameB@example.test"
$password = "M2Comments!2026"
$ruleBase = [int64]8000000000000000000 +
    ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() % 1000000000000)
$blockRuleId = $ruleBase
$reviewRuleId = $ruleBase + 1
$warnRuleId = $ruleBase + 2
$blockKeyword = "m2block$stamp"
$reviewKeyword = "m2review$stamp"
$warnKeyword = "m2warn$stamp"
$userAId = $null
$userBId = $null
$blogAId = $null
$blogBId = $null
$articleId = $null
$oldMySqlPassword = $env:MYSQL_PWD
$failure = $null

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Label
    )
    if ($Actual -ne $Expected) {
        throw "$Label expected [$Expected] but was [$Actual]"
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Label
    )
    if (-not $Condition) {
        throw "$Label"
    }
}

function Invoke-Api {
    param(
        [ValidateSet("GET", "POST", "PATCH", "PUT", "DELETE")]
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = $null,
        [int[]]$ExpectedCodes = @(200)
    )
    $headers = @{}
    if ($Token) {
        $headers["pxczxn-community-token"] = $Token
    }
    $arguments = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        TimeoutSec = 15
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 20 -Compress
        $arguments["ContentType"] = "application/json; charset=utf-8"
        $arguments["Body"] = [Text.Encoding]::UTF8.GetBytes($json)
    }
    $webResponse = Invoke-WebRequest @arguments
    $responseBytes = $webResponse.RawContentStream.ToArray()
    $responseText = [Text.Encoding]::UTF8.GetString($responseBytes)
    $response = $responseText | ConvertFrom-Json
    if ($ExpectedCodes -notcontains [int]$response.code) {
        $bodyText = $response | ConvertTo-Json -Depth 20 -Compress
        throw "$Method $Path returned unexpected business response: $bodyText"
    }
    return $response
}

function Invoke-MySql {
    param([string]$Sql)
    $output = & $MySqlPath `
        "--user=$DatabaseUser" `
        "--database=$Database" `
        "--default-character-set=utf8mb4" `
        "--batch" `
        "--skip-column-names" `
        "--execute=$Sql"
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed with exit code $LASTEXITCODE"
    }
    return $output
}

function Set-CommentScope {
    param(
        [string]$Token,
        [string]$Scope
    )
    $response = Invoke-Api `
        -Method PATCH `
        -Path "/api/v1/blogs/me/settings" `
        -Token $Token `
        -Body @{
            commentScope = $Scope
            defaultVisibility = $null
            allowRepost = $null
            themeKey = $null
            seoTitle = $null
            seoDescription = $null
        }
    Assert-Equal $response.data.commentScope $Scope "comment scope"
}

function New-Comment {
    param(
        [string]$Token,
        [string]$Content,
        [int[]]$ExpectedCodes = @(200)
    )
    return Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/ARTICLE/$articleId/comments" `
        -Token $Token `
        -Body @{ content = $Content } `
        -ExpectedCodes $ExpectedCodes
}

function Remove-Comment {
    param(
        [string]$Token,
        [string]$CommentId
    )
    return Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/comments/$CommentId" `
        -Token $Token
}

try {
    if (-not (Test-Path -LiteralPath $MySqlPath)) {
        throw "mysql client not found: $MySqlPath"
    }
    $env:MYSQL_PWD = $DatabasePassword

    $health = Invoke-Api -Method GET -Path "/api/v1/health"
    Assert-Equal $health.data.status "UP" "backend health"

    $registeredA = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/register" `
        -Body @{
            username = $usernameA
            email = $emailA
            password = $password
            displayName = "评论作者"
        }
    $registeredB = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/register" `
        -Body @{
            username = $usernameB
            email = $emailB
            password = $password
            displayName = "评论用户"
        }
    $userAId = $registeredA.data.userId
    $userBId = $registeredB.data.userId
    $blogAId = $registeredA.data.blogId
    $blogBId = $registeredB.data.blogId

    $loginA = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/login" `
        -Body @{ email = $emailA; password = $password }
    $loginB = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/login" `
        -Body @{ email = $emailB; password = $password }
    $tokenA = $loginA.data.tokenValue
    $tokenB = $loginB.data.tokenValue

    $foldersA = Invoke-Api `
        -Method GET `
        -Path "/api/v1/social/me/favorite-folders" `
        -Token $tokenA
    $foldersB = Invoke-Api `
        -Method GET `
        -Path "/api/v1/social/me/favorite-folders" `
        -Token $tokenB
    Assert-Equal $foldersA.data.Count 1 "author default favorite folder"
    Assert-Equal $foldersB.data.Count 1 "commenter default favorite folder"
    Assert-Equal $foldersA.data[0].name "全部收藏" "default folder name"

    $article = Invoke-Api `
        -Method POST `
        -Path "/api/v1/articles" `
        -Token $tokenA `
        -Body @{
            title = "M2 评论真实联调 $stamp"
            slug = "m2-comments-$stamp"
            summary = "评论、回复、范围和治理端到端验证"
            categoryId = $null
            coverFileId = $null
            contentMode = "MARKDOWN"
            richTextJson = $null
            markdownContent = "# 评论验证`n`n这是用于评论联调的公开文章。"
            visibility = "PUBLIC"
            publishMethod = "IMMEDIATE"
            tagIds = @()
            contentFileIds = @()
        }
    $articleId = $article.data.articleId
    $review = Invoke-Api `
        -Method POST `
        -Path "/api/v1/articles/$articleId/submit-review" `
        -Token $tokenA `
        -Body @{
            idempotencyKey = "m2-comments-$stamp"
            expectedLockVersion = [int]$article.data.lockVersion
        }
    Assert-Equal $review.data.publishStatus "PUBLISHED" "article publish"
    Assert-Equal $review.data.reviewStatus "APPROVED" "article review"

    $root = New-Comment `
        -Token $tokenB `
        -Content "<script>alert(1)</script>`n参考 https://example.com/comments"
    $rootId = $root.data.commentId
    Assert-Equal $root.data.status "PUBLISHED" "root comment status"
    Assert-True `
        (-not $root.data.renderedHtml.Contains("<script>")) `
        "rendered comment leaked a script tag"
    Assert-True `
        $root.data.renderedHtml.Contains("nofollow noopener noreferrer") `
        "rendered link is missing safe rel attributes"

    $reply = Invoke-Api `
        -Method POST `
        -Path "/api/v1/comments/$rootId/replies" `
        -Token $tokenB `
        -Body @{ content = "这是一条回复" }
    Assert-Equal $reply.data.rootCommentId $rootId "flat reply root"
    Assert-Equal $reply.data.parentCommentId $rootId "reply parent"

    $page = Invoke-Api `
        -Method GET `
        -Path "/api/v1/interactions/ARTICLE/$articleId/comments"
    Assert-Equal $page.data.total 1 "public root total"
    Assert-Equal $page.data.commentCount 2 "article comment count"
    Assert-Equal $page.data.records[0].replyCount 1 "reply count"
    Assert-Equal $page.data.records[0].replyPreview.Count 1 "reply preview"

    $liked = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/COMMENT/$rootId/like" `
        -Token $tokenA
    Assert-Equal $liked.data.likeCount 1 "comment like count"
    $unliked = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/interactions/COMMENT/$rootId/like" `
        -Token $tokenA
    Assert-Equal $unliked.data.likeCount 0 "comment unlike count"

    $hidden = Invoke-Api `
        -Method POST `
        -Path "/api/v1/comments/$rootId/hide" `
        -Token $tokenA `
        -Body @{ reason = "真实联调隐藏整条讨论" }
    Assert-Equal $hidden.data.status "HIDDEN_BY_AUTHOR" "hide status"
    Assert-Equal $hidden.data.affectedComments 2 "hidden thread size"
    Assert-Equal $hidden.data.targetCommentCount 0 "count after hide"
    $pageAfterHide = Invoke-Api `
        -Method GET `
        -Path "/api/v1/interactions/ARTICLE/$articleId/comments"
    Assert-Equal $pageAfterHide.data.total 0 "hidden root visibility"

    $ruleSql = @"
INSERT INTO content_keyword_rule
    (id, keyword, normalized_keyword, severity, status, description, sort_order)
VALUES
    ($blockRuleId, '$blockKeyword', '$blockKeyword', 'BLOCK', 'ACTIVE', 'M2-T005 E2E', 1),
    ($reviewRuleId, '$reviewKeyword', '$reviewKeyword', 'REVIEW', 'ACTIVE', 'M2-T005 E2E', 2),
    ($warnRuleId, '$warnKeyword', '$warnKeyword', 'WARN', 'ACTIVE', 'M2-T005 E2E', 3);
"@
    $null = Invoke-MySql -Sql $ruleSql

    $blocked = New-Comment `
        -Token $tokenB `
        -Content "包含 $blockKeyword 的评论" `
        -ExpectedCodes @(400)
    Assert-Equal $blocked.code 400 "block keyword"

    $pending = New-Comment `
        -Token $tokenB `
        -Content "包含 $reviewKeyword 的评论"
    Assert-Equal $pending.data.status "PENDING_REVIEW" "review keyword"
    $afterPending = Invoke-Api `
        -Method GET `
        -Path "/api/v1/interactions/ARTICLE/$articleId/comments"
    Assert-Equal $afterPending.data.commentCount 0 "pending comment count"

    $warned = New-Comment `
        -Token $tokenB `
        -Content "包含 $warnKeyword 的评论"
    Assert-Equal $warned.data.status "PUBLISHED" "warn keyword status"
    Assert-True $warned.data.moderationWarning "warn result flag"
    Assert-Equal `
        $warned.data.moderationResult `
        "AUTO_APPROVED_WITH_WARNING" `
        "warn moderation result"
    $removedWarn = Remove-Comment `
        -Token $tokenB `
        -CommentId $warned.data.commentId
    Assert-Equal $removedWarn.data.targetCommentCount 0 "warn delete count"

    Set-CommentScope -Token $tokenA -Scope "FOLLOWERS_ONLY"
    $followersDenied = New-Comment `
        -Token $tokenB `
        -Content "关注前禁止" `
        -ExpectedCodes @(403)
    Assert-Equal $followersDenied.code 403 "followers-only before follow"
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/blogs/$blogAId/follow" `
        -Token $tokenB `
        -Body @{ notificationLevel = "ALL"; specialFollow = $false }
    $followersAllowed = New-Comment `
        -Token $tokenB `
        -Content "关注后允许"
    $null = Remove-Comment `
        -Token $tokenB `
        -CommentId $followersAllowed.data.commentId

    Set-CommentScope -Token $tokenA -Scope "MUTUAL_ONLY"
    $mutualDenied = New-Comment `
        -Token $tokenB `
        -Content "互关前禁止" `
        -ExpectedCodes @(403)
    Assert-Equal $mutualDenied.code 403 "mutual-only before mutual follow"
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/blogs/$blogBId/follow" `
        -Token $tokenA `
        -Body @{ notificationLevel = "ALL"; specialFollow = $false }
    $mutualAllowed = New-Comment `
        -Token $tokenB `
        -Content "互关后允许"
    $null = Remove-Comment `
        -Token $tokenB `
        -CommentId $mutualAllowed.data.commentId

    Set-CommentScope -Token $tokenA -Scope "BLOGGER_FOLLOWING"
    $followingAllowed = New-Comment `
        -Token $tokenB `
        -Content "博主关注的人允许"
    $null = Remove-Comment `
        -Token $tokenB `
        -CommentId $followingAllowed.data.commentId

    Set-CommentScope -Token $tokenA -Scope "DISABLED"
    $disabled = New-Comment `
        -Token $tokenB `
        -Content "关闭评论后禁止" `
        -ExpectedCodes @(403)
    Assert-Equal $disabled.code 403 "disabled comments"

    Set-CommentScope -Token $tokenA -Scope "ALL_LOGGED_IN"
    $ownComment = New-Comment `
        -Token $tokenB `
        -Content "用于幂等删除"
    $firstDelete = Remove-Comment `
        -Token $tokenB `
        -CommentId $ownComment.data.commentId
    $secondDelete = Remove-Comment `
        -Token $tokenB `
        -CommentId $ownComment.data.commentId
    Assert-Equal $firstDelete.data.affectedComments 1 "first user delete"
    Assert-Equal $secondDelete.data.affectedComments 0 "repeated user delete"

    $null = Invoke-MySql -Sql @"
UPDATE community_user
SET comment_restricted_until = UTC_TIMESTAMP(3) + INTERVAL 1 HOUR
WHERE id = $userBId;
"@
    $restricted = New-Comment `
        -Token $tokenB `
        -Content "限制期间禁止" `
        -ExpectedCodes @(403)
    Assert-Equal $restricted.code 403 "comment restriction"
    $null = Invoke-MySql -Sql @"
UPDATE community_user
SET comment_restricted_until = NULL
WHERE id = $userBId;
"@

    $databaseEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'events=', COUNT(*),
    ', published=', SUM(new_status = 'PUBLISHED'),
    ', pending=', SUM(new_status = 'PENDING_REVIEW'),
    ', hidden=', SUM(new_status IN ('HIDDEN_BY_AUTHOR', 'HIDDEN_BY_BLOG')),
    ', deleted=', SUM(new_status = 'DELETED_BY_USER')
)
FROM community_comment_moderation_event e
JOIN community_comment c ON c.id = e.comment_id
WHERE c.target_type = 'ARTICLE' AND c.target_id = $articleId;
"@

    [ordered]@{
        backend = "UP"
        articleStatus = $review.data.publishStatus
        safeRendering = $true
        rootAndReplyCount = 2
        commentLikeCount = 1
        hiddenThreadCount = 2
        blockCode = [int]$blocked.code
        reviewStatus = $pending.data.status
        warningStatus = $warned.data.moderationResult
        followersBeforeAfter = "403/200"
        mutualBeforeAfter = "403/200"
        bloggerFollowing = 200
        disabledCode = [int]$disabled.code
        repeatedDelete = "1/0"
        restrictedCode = [int]$restricted.code
        databaseEvidence = ($databaseEvidence -join "")
    } | ConvertTo-Json -Depth 10
}
catch {
    $failure = $_
    Write-Host "E2E_FAILED: $($_.Exception.Message)"
}
finally {
    try {
        $env:MYSQL_PWD = $DatabasePassword
        $cleanupSql = @"
SET @user_a := (
    SELECT id FROM community_user
    WHERE username = '$usernameA' LIMIT 1
);
SET @user_b := (
    SELECT id FROM community_user
    WHERE username = '$usernameB' LIMIT 1
);
SET @blog_a := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_a AND blog_type = 'PERSONAL' LIMIT 1
);
SET @blog_b := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_b AND blog_type = 'PERSONAL' LIMIT 1
);
SET @article_id := (
    SELECT id FROM article
    WHERE author_user_id = @user_a
      AND slug = 'm2-comments-$stamp'
    LIMIT 1
);

DELETE FROM community_comment_moderation_event
WHERE comment_id IN (
    SELECT id FROM community_comment
    WHERE target_type = 'ARTICLE' AND target_id = @article_id
);
DELETE FROM community_content_like
WHERE user_id IN (@user_a, @user_b)
   OR (
        target_type = 'COMMENT'
        AND target_id IN (
            SELECT id FROM community_comment
            WHERE target_type = 'ARTICLE' AND target_id = @article_id
        )
   );
DELETE FROM community_comment
WHERE target_type = 'ARTICLE'
  AND target_id = @article_id
  AND root_comment_id IS NOT NULL;
DELETE FROM community_comment
WHERE target_type = 'ARTICLE'
  AND target_id = @article_id;
DELETE FROM content_keyword_rule
WHERE id IN ($blockRuleId, $reviewRuleId, $warnRuleId);
DELETE FROM community_follow
WHERE follower_user_id IN (@user_a, @user_b)
   OR (
        target_type = 'BLOG'
        AND target_id IN (@blog_a, @blog_b)
   );
DELETE FROM favorite_folder_item
WHERE folder_id IN (
    SELECT id FROM favorite_folder
    WHERE owner_user_id IN (@user_a, @user_b)
);
DELETE FROM favorite_item
WHERE owner_user_id IN (@user_a, @user_b);
DELETE FROM favorite_folder
WHERE owner_user_id IN (@user_a, @user_b);
DELETE FROM community_notification
WHERE sender_user_id IN (@user_a, @user_b)
   OR id IN (
        SELECT notification_id
        FROM community_notification_recipient
        WHERE recipient_user_id IN (@user_a, @user_b)
   );
DELETE FROM article_publish_task
WHERE article_id = @article_id;
DELETE FROM content_review_task
WHERE article_id = @article_id;
DELETE FROM article_tag
WHERE article_id = @article_id;
DELETE FROM community_file_reference
WHERE owner_user_id IN (@user_a, @user_b)
   OR (
        target_type IN ('ARTICLE', 'ARTICLE_VERSION')
        AND target_id = @article_id
   );
DELETE FROM article
WHERE id = @article_id;
DELETE FROM blog_category
WHERE blog_id IN (@blog_a, @blog_b);
UPDATE community_user
SET personal_blog_id = NULL
WHERE id IN (@user_a, @user_b);
DELETE FROM blog
WHERE id IN (@blog_a, @blog_b);
DELETE FROM community_user
WHERE id IN (@user_a, @user_b);
"@
        $null = Invoke-MySql -Sql $cleanupSql
        $cleanupEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'users=', (
        SELECT COUNT(*) FROM community_user
        WHERE username IN ('$usernameA', '$usernameB')
    ),
    ', articles=', (
        SELECT COUNT(*) FROM article
        WHERE slug = 'm2-comments-$stamp'
    ),
    ', rules=', (
        SELECT COUNT(*) FROM content_keyword_rule
        WHERE id IN ($blockRuleId, $reviewRuleId, $warnRuleId)
    ),
    ', comments=', (
        SELECT COUNT(*) FROM community_comment c
        JOIN article a ON a.id = c.target_id
        WHERE c.target_type = 'ARTICLE'
          AND a.slug = 'm2-comments-$stamp'
    )
);
"@
        Write-Host "CLEANUP: $($cleanupEvidence -join '')"
    }
    catch {
        Write-Host "CLEANUP_FAILED: $($_.Exception.Message)"
        if ($null -eq $failure) {
            $failure = $_
        }
    }
    if ($null -eq $oldMySqlPassword) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
    else {
        $env:MYSQL_PWD = $oldMySqlPassword
    }
}

if ($null -ne $failure) {
    throw $failure
}
