param(
    [string]$BaseUrl = "http://127.0.0.1:8849",
    [string]$Database = "pxczxn_community",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "MMddHHmmssfff"
$usernameA = "m2ma_$stamp"
$usernameB = "m2mb_$stamp"
$emailA = "$usernameA@example.test"
$emailB = "$usernameB@example.test"
$password = "M2Moments!2026"
$ruleBase = [int64]8100000000000000000 +
    ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() % 1000000000000)
$blockRuleId = $ruleBase
$reviewRuleId = $ruleBase + 1
$warnRuleId = $ruleBase + 2
$blockKeyword = "momentblock$stamp"
$reviewKeyword = "momentreview$stamp"
$warnKeyword = "momentwarn$stamp"
$userAId = $null
$userBId = $null
$blogAId = $null
$blogBId = $null
$articleId = $null
$oldMySqlPassword = $env:MYSQL_PWD
$failure = $null

function Assert-Equal {
    param($Actual, $Expected, [string]$Label)
    if ($Actual -ne $Expected) {
        throw "$Label expected [$Expected] but was [$Actual]"
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Label)
    if (-not $Condition) {
        throw $Label
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

function New-Moment {
    param(
        [string]$Token,
        [string]$Type,
        [string]$Text = $null,
        [string]$Link = $null,
        [string]$Article = $null,
        [string]$Source = $null,
        [string]$Visibility = "PUBLIC",
        [int[]]$ExpectedCodes = @(200)
    )
    return Invoke-Api `
        -Method POST `
        -Path "/api/v1/moments" `
        -Token $Token `
        -ExpectedCodes $ExpectedCodes `
        -Body @{
            blogId = $null
            momentType = $Type
            textContent = $Text
            linkUrl = $Link
            articleId = $Article
            repostMomentId = $Source
            visibility = $Visibility
        }
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
            displayName = "动态作者"
        }
    $registeredB = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/register" `
        -Body @{
            username = $usernameB
            email = $emailB
            password = $password
            displayName = "动态读者"
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

    $article = Invoke-Api `
        -Method POST `
        -Path "/api/v1/articles" `
        -Token $tokenA `
        -Body @{
            title = "M2 动态真实联调 $stamp"
            slug = "m2-moments-$stamp"
            summary = "动态关联文章与访问控制联调"
            categoryId = $null
            coverFileId = $null
            contentMode = "MARKDOWN"
            richTextJson = $null
            markdownContent = "# 动态验证`n`n这是用于动态联调的公开文章。"
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
            idempotencyKey = "m2-moments-$stamp"
            expectedLockVersion = [int]$article.data.lockVersion
        }
    Assert-Equal $review.data.publishStatus "PUBLISHED" "article publish"

    $text = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "<script>alert(1)</script>`n参考 https://example.com/moments"
    $textId = $text.data.moment.momentId
    Assert-Equal $text.data.moment.status "PUBLISHED" "text status"
    Assert-True `
        (-not $text.data.moment.renderedHtml.Contains("<script>")) `
        "rendered moment leaked a script tag"
    Assert-True `
        $text.data.moment.renderedHtml.Contains("nofollow noopener noreferrer") `
        "rendered moment link is missing safe rel attributes"

    $link = New-Moment `
        -Token $tokenA `
        -Type "LINK" `
        -Text "可信外链" `
        -Link "https://example.com/resource"
    Assert-Equal $link.data.moment.linkUrl "https://example.com/resource" "link URL"
    $unsafeLink = New-Moment `
        -Token $tokenA `
        -Type "LINK" `
        -Link "javascript:alert(1)" `
        -ExpectedCodes @(400)
    Assert-Equal $unsafeLink.code 400 "unsafe link"

    $articleShare = New-Moment `
        -Token $tokenA `
        -Type "ARTICLE_SHARE" `
        -Article $articleId
    Assert-True $articleShare.data.moment.article.available "article card"
    Assert-Equal $articleShare.data.moment.article.articleId $articleId "article ID"

    $repost = New-Moment `
        -Token $tokenB `
        -Type "REPOST" `
        -Source $textId
    $repostId = $repost.data.moment.momentId
    Assert-Equal $repost.data.moment.repostSource.momentId $textId "repost source"
    Assert-Equal $repost.data.moment.repostSource.available $true "repost source available"

    $quote = New-Moment `
        -Token $tokenB `
        -Type "QUOTE" `
        -Text "我认同这个观点" `
        -Source $textId
    $quoteId = $quote.data.moment.momentId
    $sourceAfterReposts = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$textId"
    Assert-Equal $sourceAfterReposts.data.repostCount 2 "source repost count"

    $repostWithText = New-Moment `
        -Token $tokenB `
        -Type "REPOST" `
        -Text "纯转发不应有正文" `
        -Source $textId `
        -ExpectedCodes @(400)
    Assert-Equal $repostWithText.code 400 "repost text shape"
    $quoteWithoutText = New-Moment `
        -Token $tokenB `
        -Type "QUOTE" `
        -Source $textId `
        -ExpectedCodes @(400)
    Assert-Equal $quoteWithoutText.code 400 "quote text shape"

    $shareLink = Invoke-Api `
        -Method POST `
        -Path "/api/v1/moments/$textId/share-link"
    Assert-Equal $shareLink.data.canonicalPath "/moments/$textId" "share path"

    $liked = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    Assert-Equal $liked.data.likeCount 1 "moment like"
    $favorite = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/favorite" `
        -Token $tokenB `
        -Body @{ folderIds = @() }
    Assert-Equal $favorite.data.favoriteCount 1 "moment favorite"
    $comment = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/comments" `
        -Token $tokenB `
        -Body @{ content = "动态评论" }
    $commentId = $comment.data.commentId
    $interactive = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$textId" `
        -Token $tokenB
    Assert-Equal $interactive.data.likeCount 1 "detail like count"
    Assert-Equal $interactive.data.favoriteCount 1 "detail favorite count"
    Assert-Equal $interactive.data.commentCount 1 "detail comment count"
    Assert-Equal $interactive.data.liked $true "detail liked"
    Assert-Equal $interactive.data.favorited $true "detail favorited"
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/interactions/MOMENT/$textId/favorite" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/comments/$commentId" `
        -Token $tokenB

    $feed = Invoke-Api -Method GET -Path "/api/v1/moments?pageSize=50"
    Assert-True ($feed.data.total -ge 5) "public feed is missing published moments"
    $blogFeed = Invoke-Api `
        -Method GET `
        -Path "/api/v1/blogs/$blogAId/moments?pageSize=50"
    Assert-True ($blogFeed.data.total -ge 3) "blog moment list"
    $mine = Invoke-Api `
        -Method GET `
        -Path "/api/v1/social/me/moments?pageSize=50" `
        -Token $tokenB
    Assert-True ($mine.data.total -ge 2) "my moment list"

    $followersOnly = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "仅关注者可见" `
        -Visibility "FOLLOWERS_ONLY"
    $followersOnlyId = $followersOnly.data.moment.momentId
    $anonymousDenied = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$followersOnlyId" `
        -ExpectedCodes @(404)
    Assert-Equal $anonymousDenied.code 404 "anonymous follower-only detail"
    $beforeFollow = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$followersOnlyId" `
        -Token $tokenB `
        -ExpectedCodes @(404)
    Assert-Equal $beforeFollow.code 404 "follower-only before follow"
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/blogs/$blogAId/follow" `
        -Token $tokenB `
        -Body @{ notificationLevel = "ALL"; specialFollow = $false }
    $afterFollow = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$followersOnlyId" `
        -Token $tokenB
    Assert-Equal $afterFollow.data.visibility "FOLLOWERS_ONLY" "follower-only after follow"

    $restrictedWrapper = New-Moment `
        -Token $tokenB `
        -Type "REPOST" `
        -Source $followersOnlyId `
        -Visibility "PUBLIC"
    $restrictedWrapperId = $restrictedWrapper.data.moment.momentId
    $wrapperAnonymousDenied = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$restrictedWrapperId" `
        -ExpectedCodes @(404)
    Assert-Equal $wrapperAnonymousDenied.code 404 "repost visibility broadening"

    $private = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "仅自己可见" `
        -Visibility "PRIVATE"
    $privateId = $private.data.moment.momentId
    $privateOwner = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$privateId" `
        -Token $tokenA
    Assert-Equal $privateOwner.data.visibility "PRIVATE" "private owner detail"
    $privateOther = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$privateId" `
        -Token $tokenB `
        -ExpectedCodes @(404)
    Assert-Equal $privateOther.code 404 "private other detail"

    $null = Invoke-MySql -Sql @"
UPDATE community_user
SET publish_restricted_until = UTC_TIMESTAMP(3) + INTERVAL 1 HOUR
WHERE id = $userAId;
"@
    $restrictedPublish = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "限制期间禁止发布" `
        -ExpectedCodes @(403)
    Assert-Equal $restrictedPublish.code 403 "publish restriction"
    $null = Invoke-MySql -Sql @"
UPDATE community_user
SET publish_restricted_until = NULL
WHERE id = $userAId;
"@

    $null = Invoke-MySql -Sql @"
INSERT INTO content_keyword_rule
    (id, keyword, normalized_keyword, severity, content_scopes, risk_level, hit_action, status, description, sort_order)
VALUES
    ($blockRuleId, '$blockKeyword', '$blockKeyword', 'BLOCK', 'MOMENT', 'CRITICAL', 'BLOCK', 'ACTIVE', 'M2-T006 E2E', 1),
    ($reviewRuleId, '$reviewKeyword', '$reviewKeyword', 'REVIEW', 'MOMENT', 'HIGH', 'MANUAL_REVIEW', 'ACTIVE', 'M2-T006 E2E', 2),
    ($warnRuleId, '$warnKeyword', '$warnKeyword', 'WARN', 'MOMENT', 'MEDIUM', 'WARN', 'ACTIVE', 'M2-T006 E2E', 3);
"@
    $blocked = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "包含 $blockKeyword 的动态" `
        -ExpectedCodes @(400)
    Assert-Equal $blocked.code 400 "block keyword"
    $pending = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "包含 $reviewKeyword 的动态"
    Assert-Equal $pending.data.moment.status "PENDING_REVIEW" "review keyword"
    $pendingPublic = Invoke-Api `
        -Method GET `
        -Path "/api/v1/moments/$($pending.data.moment.momentId)" `
        -ExpectedCodes @(404)
    Assert-Equal $pendingPublic.code 404 "pending public visibility"
    $warned = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "包含 $warnKeyword 的动态"
    Assert-Equal $warned.data.moment.status "PUBLISHED" "warn status"
    Assert-True $warned.data.moderationWarning "warn flag"
    Assert-Equal `
        $warned.data.moderationResult `
        "AUTO_APPROVED_WITH_WARNING" `
        "warn result"

    $deleteQuote = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/moments/${quoteId}?expectedLockVersion=0" `
        -Token $tokenB
    Assert-Equal $deleteQuote.data.sourceRepostCount 1 "quote deletion source count"
    $deleteQuoteAgain = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/moments/${quoteId}?expectedLockVersion=0" `
        -Token $tokenB
    Assert-Equal $deleteQuoteAgain.data.idempotentReplay $true "quote repeated delete"
    $deleteRepost = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/moments/${repostId}?expectedLockVersion=0" `
        -Token $tokenB
    Assert-Equal $deleteRepost.data.sourceRepostCount 0 "repost deletion source count"
    $deleteRepostAgain = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/moments/${repostId}?expectedLockVersion=0" `
        -Token $tokenB
    Assert-Equal $deleteRepostAgain.data.idempotentReplay $true "repost repeated delete"

    $databaseEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'moments=', COUNT(*),
    ', published=', SUM(status = 'PUBLISHED'),
    ', pending=', SUM(status = 'PENDING_REVIEW'),
    ', deleted=', SUM(status = 'DELETED'),
    ', sourceReposts=', (
        SELECT repost_count FROM community_moment WHERE id = $textId
    )
)
FROM community_moment
WHERE actor_user_id IN ($userAId, $userBId);
"@
    $constraintEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'feedIndex=', (
        SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'community_moment'
          AND index_name = 'idx_community_moment_public_feed'
    ),
    ', shapeChecks=', (
        SELECT COUNT(*) FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'community_moment'
          AND constraint_type = 'CHECK'
          AND constraint_name IN (
              'chk_community_moment_link_required',
              'chk_community_moment_quote_text',
              'chk_community_moment_repost_text'
          )
    )
);
"@

    [ordered]@{
        backend = "UP"
        safeRendering = $true
        linkSafety = "https-only"
        articleCard = $true
        repostAndQuoteCount = 2
        shapeValidation = "400/400"
        sharePath = $shareLink.data.canonicalPath
        interactionCounts = "1/1/1"
        publicAndBlogFeeds = "visible"
        followersBeforeAfter = "404/200"
        restrictedSourceBroadening = 404
        privateOwnerOther = "200/404"
        publishRestriction = 403
        moderation = "BLOCK400/REVIEW_PENDING/WARN_PUBLISHED"
        repeatedDelete = "true/true"
        databaseEvidence = ($databaseEvidence -join "")
        constraints = ($constraintEvidence -join "")
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
      AND slug = 'm2-moments-$stamp'
    LIMIT 1
);

DELETE FROM community_comment_moderation_event
WHERE comment_id IN (
    SELECT id FROM community_comment
    WHERE target_type = 'MOMENT'
      AND target_id IN (
          SELECT id FROM community_moment
          WHERE actor_user_id IN (@user_a, @user_b)
      )
);
DELETE FROM community_content_like
WHERE user_id IN (@user_a, @user_b)
   OR (
        target_type = 'MOMENT'
        AND target_id IN (
            SELECT id FROM community_moment
            WHERE actor_user_id IN (@user_a, @user_b)
        )
   )
   OR (
        target_type = 'COMMENT'
        AND target_id IN (
            SELECT id FROM community_comment
            WHERE target_type = 'MOMENT'
              AND target_id IN (
                  SELECT id FROM community_moment
                  WHERE actor_user_id IN (@user_a, @user_b)
              )
        )
   );
DELETE FROM community_comment
WHERE target_type = 'MOMENT'
  AND target_id IN (
      SELECT id FROM community_moment
      WHERE actor_user_id IN (@user_a, @user_b)
  )
  AND root_comment_id IS NOT NULL;
DELETE FROM community_comment
WHERE target_type = 'MOMENT'
  AND target_id IN (
      SELECT id FROM community_moment
      WHERE actor_user_id IN (@user_a, @user_b)
  );
DELETE FROM favorite_folder_item
WHERE favorite_item_id IN (
    SELECT id FROM favorite_item
    WHERE owner_user_id IN (@user_a, @user_b)
       OR (
            target_type = 'MOMENT'
            AND target_id IN (
                SELECT id FROM community_moment
                WHERE actor_user_id IN (@user_a, @user_b)
            )
       )
);
DELETE FROM favorite_item
WHERE owner_user_id IN (@user_a, @user_b)
   OR (
        target_type = 'MOMENT'
        AND target_id IN (
            SELECT id FROM community_moment
            WHERE actor_user_id IN (@user_a, @user_b)
        )
   );
DELETE FROM community_moment
WHERE actor_user_id IN (@user_a, @user_b);
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
        WHERE slug = 'm2-moments-$stamp'
    ),
    ', moments=', (
        SELECT COUNT(*) FROM community_moment
        WHERE actor_user_id IN (
            SELECT id FROM community_user
            WHERE username IN ('$usernameA', '$usernameB')
        )
    ),
    ', rules=', (
        SELECT COUNT(*) FROM content_keyword_rule
        WHERE id IN ($blockRuleId, $reviewRuleId, $warnRuleId)
    ),
    ', comments=', (
        SELECT COUNT(*) FROM community_comment
        WHERE target_type = 'MOMENT'
          AND target_id IN (
              SELECT id FROM community_moment
              WHERE actor_user_id IN (
                  SELECT id FROM community_user
                  WHERE username IN ('$usernameA', '$usernameB')
              )
          )
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
