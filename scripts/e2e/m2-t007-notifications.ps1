param(
    [string]$BaseUrl = "http://127.0.0.1:8849",
    [string]$Database = "mars-system",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "MMddHHmmssfff"
$usernameA = "m2na_$stamp"
$usernameB = "m2nb_$stamp"
$usernameC = "m2nc_$stamp"
$usernameD = "m2nd_$stamp"
$emailA = "$usernameA@example.test"
$emailB = "$usernameB@example.test"
$emailC = "$usernameC@example.test"
$emailD = "$usernameD@example.test"
$password = "M2Notifications!2026"
$userAId = $null
$userBId = $null
$userCId = $null
$userDId = $null
$blogAId = $null
$blogBId = $null
$blogCId = $null
$blogDId = $null
$textId = $null
$projectId = $null
$repostId = $null
$rootCommentId = $null
$replyCommentId = $null
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

function Register-And-Login {
    param(
        [string]$Username,
        [string]$Email,
        [string]$DisplayName
    )
    $registered = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/register" `
        -Body @{
            username = $Username
            email = $Email
            password = $password
            displayName = $DisplayName
        }
    $login = Invoke-Api `
        -Method POST `
        -Path "/api/v1/auth/login" `
        -Body @{ email = $Email; password = $password }
    return [ordered]@{
        userId = $registered.data.userId
        blogId = $registered.data.blogId
        token = $login.data.tokenValue
    }
}

function Follow-Blog {
    param(
        [string]$Token,
        [string]$BlogId,
        [string]$Level,
        [bool]$Special = $false
    )
    return Invoke-Api `
        -Method POST `
        -Path "/api/v1/blogs/$BlogId/follow" `
        -Token $Token `
        -Body @{
            notificationLevel = $Level
            specialFollow = $Special
        }
}

function New-Moment {
    param(
        [string]$Token,
        [string]$Type,
        [string]$Text = $null,
        [string]$Source = $null
    )
    return Invoke-Api `
        -Method POST `
        -Path "/api/v1/moments" `
        -Token $Token `
        -Body @{
            blogId = $null
            momentType = $Type
            textContent = $Text
            linkUrl = $null
            articleId = $null
            repostMomentId = $Source
            visibility = "PUBLIC"
        }
}

function Get-Notifications {
    param(
        [string]$Token,
        [string]$Category = $null,
        [string]$Status = $null
    )
    $query = "?pageNum=1&pageSize=50"
    if ($Category) {
        $query += "&category=$Category"
    }
    if ($Status) {
        $query += "&status=$Status"
    }
    return Invoke-Api `
        -Method GET `
        -Path "/api/v1/notifications$query" `
        -Token $Token
}

try {
    if (-not (Test-Path -LiteralPath $MySqlPath)) {
        throw "mysql client not found: $MySqlPath"
    }
    $env:MYSQL_PWD = $DatabasePassword

    $health = Invoke-Api -Method GET -Path "/api/v1/health"
    Assert-Equal $health.data.status "UP" "backend health"
    $anonymous = Invoke-Api `
        -Method GET `
        -Path "/api/v1/notifications" `
        -ExpectedCodes @(401)
    Assert-Equal $anonymous.code 401 "anonymous inbox"

    # Avoid a fixed ten-minute aggregation bucket boundary during this run.
    $epoch = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $secondsToBoundary = 600 - ($epoch % 600)
    if ($secondsToBoundary -lt 20) {
        Start-Sleep -Seconds ([int]$secondsToBoundary + 1)
    }

    $accountA = Register-And-Login `
        -Username $usernameA `
        -Email $emailA `
        -DisplayName "通知作者"
    $accountB = Register-And-Login `
        -Username $usernameB `
        -Email $emailB `
        -DisplayName "全部关注"
    $accountC = Register-And-Login `
        -Username $usernameC `
        -Email $emailC `
        -DisplayName "重要关注"
    $accountD = Register-And-Login `
        -Username $usernameD `
        -Email $emailD `
        -DisplayName "静默关注"
    $userAId = $accountA.userId
    $userBId = $accountB.userId
    $userCId = $accountC.userId
    $userDId = $accountD.userId
    $blogAId = $accountA.blogId
    $blogBId = $accountB.blogId
    $blogCId = $accountC.blogId
    $blogDId = $accountD.blogId
    $tokenA = $accountA.token
    $tokenB = $accountB.token
    $tokenC = $accountC.token
    $tokenD = $accountD.token

    $null = Follow-Blog `
        -Token $tokenB `
        -BlogId $blogAId `
        -Level "ALL"
    $null = Follow-Blog `
        -Token $tokenC `
        -BlogId $blogAId `
        -Level "IMPORTANT"
    $null = Follow-Blog `
        -Token $tokenD `
        -BlogId $blogAId `
        -Level "MUTED"

    $authorFollowInbox = Get-Notifications `
        -Token $tokenA `
        -Category "FOLLOW"
    Assert-Equal $authorFollowInbox.data.total 1 "aggregated follow records"
    Assert-Equal `
        $authorFollowInbox.data.records[0].aggregateCount `
        3 `
        "aggregated follow count"
    $authorInitialUnread = Invoke-Api `
        -Method GET `
        -Path "/api/v1/notifications/unread-count" `
        -Token $tokenA
    Assert-Equal $authorInitialUnread.data.total 1 "initial unread"

    $text = New-Moment `
        -Token $tokenA `
        -Type "TEXT" `
        -Text "用于通知联调的公开动态"
    $textId = $text.data.moment.momentId
    $project = New-Moment `
        -Token $tokenA `
        -Type "PROJECT_UPDATE" `
        -Text "这是一个重要项目进展"
    $projectId = $project.data.moment.momentId

    $allFollowerInbox = Get-Notifications `
        -Token $tokenB `
        -Category "INTERACTION"
    Assert-Equal $allFollowerInbox.data.total 1 "ALL follower publication record"
    Assert-Equal `
        $allFollowerInbox.data.records[0].notificationType `
        "MOMENT_PUBLISHED" `
        "ALL follower publication type"
    Assert-Equal `
        $allFollowerInbox.data.records[0].aggregateCount `
        2 `
        "ALL follower publication aggregate"
    Assert-Equal `
        $allFollowerInbox.data.records[0].importance `
        "HIGH" `
        "important publication upgrades aggregate"

    $importantFollowerInbox = Get-Notifications `
        -Token $tokenC `
        -Category "INTERACTION"
    Assert-Equal `
        $importantFollowerInbox.data.total `
        1 `
        "IMPORTANT follower records"
    Assert-Equal `
        $importantFollowerInbox.data.records[0].targetId `
        $projectId `
        "IMPORTANT follower only receives project update"
    Assert-Equal `
        $importantFollowerInbox.data.records[0].importance `
        "HIGH" `
        "IMPORTANT follower importance"

    $mutedFollowerInbox = Get-Notifications -Token $tokenD
    Assert-Equal $mutedFollowerInbox.data.total 0 "MUTED follower inbox"

    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/favorite" `
        -Token $tokenB `
        -Body @{ folderIds = @() }
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/favorite" `
        -Token $tokenB `
        -Body @{ folderIds = @() }
    $root = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/comments" `
        -Token $tokenB `
        -Body @{
            content = "这条评论提到了 @$usernameC"
        }
    $rootCommentId = $root.data.commentId
    $reply = Invoke-Api `
        -Method POST `
        -Path "/api/v1/comments/$rootCommentId/replies" `
        -Token $tokenC `
        -Body @{ content = "这是对评论者的回复" }
    $replyCommentId = $reply.data.commentId
    $repost = New-Moment `
        -Token $tokenB `
        -Type "REPOST" `
        -Source $textId
    $repostId = $repost.data.moment.momentId

    # Self-interaction updates the content count but must not create a notification.
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenA
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenA

    $authorUnread = Invoke-Api `
        -Method GET `
        -Path "/api/v1/notifications/unread-count" `
        -Token $tokenA
    Assert-Equal $authorUnread.data.total 5 "author unread total"
    Assert-Equal $authorUnread.data.categories.FOLLOW 1 "follow unread"
    Assert-Equal `
        $authorUnread.data.categories.INTERACTION `
        3 `
        "interaction unread"
    Assert-Equal $authorUnread.data.categories.COMMENT 1 "comment unread"

    $authorInteractions = Get-Notifications `
        -Token $tokenA `
        -Category "INTERACTION"
    Assert-Equal $authorInteractions.data.total 3 "author interaction records"
    $likeNotification = $authorInteractions.data.records |
        Where-Object { $_.notificationType -eq "LIKE" } |
        Select-Object -First 1
    Assert-True ($null -ne $likeNotification) "like notification missing"
    Assert-Equal `
        $likeNotification.aggregateCount `
        1 `
        "idempotent repeated like did not aggregate"

    $authorComments = Get-Notifications `
        -Token $tokenA `
        -Category "COMMENT"
    Assert-Equal $authorComments.data.total 1 "author comment records"
    Assert-Equal `
        $authorComments.data.records[0].aggregateCount `
        2 `
        "comment and reply aggregate"

    $mentionInbox = Get-Notifications `
        -Token $tokenC `
        -Category "COMMENT"
    Assert-Equal $mentionInbox.data.total 1 "mention record"
    Assert-Equal `
        $mentionInbox.data.records[0].notificationType `
        "MENTION" `
        "mention type"
    $replyInbox = Get-Notifications `
        -Token $tokenB `
        -Category "COMMENT"
    Assert-Equal $replyInbox.data.total 1 "reply record"
    Assert-Equal `
        $replyInbox.data.records[0].notificationType `
        "REPLY" `
        "reply type"

    $firstRead = Invoke-Api `
        -Method PATCH `
        -Path "/api/v1/notifications/$($likeNotification.notificationId)/read" `
        -Token $tokenA
    Assert-Equal $firstRead.data.idempotentReplay $false "first read"
    Assert-Equal $firstRead.data.unreadCount 4 "unread after first read"
    $secondRead = Invoke-Api `
        -Method PATCH `
        -Path "/api/v1/notifications/$($likeNotification.notificationId)/read" `
        -Token $tokenA
    Assert-Equal $secondRead.data.idempotentReplay $true "repeated read"
    Assert-Equal $secondRead.data.unreadCount 4 "unread after replay"

    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method POST `
        -Path "/api/v1/interactions/MOMENT/$textId/like" `
        -Token $tokenB
    $authorInteractionsAfterRelike = Get-Notifications `
        -Token $tokenA `
        -Category "INTERACTION"
    $likeAfterRelike = $authorInteractionsAfterRelike.data.records |
        Where-Object { $_.notificationType -eq "LIKE" } |
        Select-Object -First 1
    Assert-Equal $likeAfterRelike.aggregateCount 2 "relike aggregate"
    Assert-Equal $likeAfterRelike.status "UNREAD" "relike reopens unread"

    $readInteractions = Invoke-Api `
        -Method PATCH `
        -Path "/api/v1/notifications/read-all?category=INTERACTION" `
        -Token $tokenA
    Assert-Equal `
        $readInteractions.data.affectedNotifications `
        3 `
        "read all interactions"
    Assert-Equal `
        $readInteractions.data.unreadCount `
        2 `
        "remaining follow and comment"
    $readEverything = Invoke-Api `
        -Method PATCH `
        -Path "/api/v1/notifications/read-all" `
        -Token $tokenA
    Assert-Equal `
        $readEverything.data.affectedNotifications `
        2 `
        "read all remaining"
    Assert-Equal $readEverything.data.unreadCount 0 "final unread"

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
        -Path "/api/v1/blogs/$blogAId/follow" `
        -Token $tokenB
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/blogs/$blogAId/follow" `
        -Token $tokenC
    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/blogs/$blogAId/follow" `
        -Token $tokenD
    $afterCancellation = Invoke-Api `
        -Method GET `
        -Path "/api/v1/notifications/unread-count" `
        -Token $tokenA
    Assert-Equal `
        $afterCancellation.data.total `
        0 `
        "cancellations create no notification"

    $null = Invoke-Api `
        -Method DELETE `
        -Path "/api/v1/moments/${textId}?expectedLockVersion=0" `
        -Token $tokenA
    $redactedInteractions = Get-Notifications `
        -Token $tokenA `
        -Category "INTERACTION"
    Assert-Equal $redactedInteractions.data.total 3 "redacted record count"
    foreach ($record in $redactedInteractions.data.records) {
        Assert-Equal `
            $record.targetAvailable `
            $false `
            "deleted target availability"
        Assert-Equal $record.sender $null "deleted target sender"
        Assert-Equal $record.targetId $null "deleted target id"
        Assert-Equal $record.canonicalPath $null "deleted target path"
    }

    $databaseEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'notifications=', COUNT(*),
    ', recipients=', (
        SELECT COUNT(*)
        FROM community_notification_recipient
        WHERE recipient_user_id IN (
            $userAId, $userBId, $userCId, $userDId
        )
    ),
    ', aggregateMax=', MAX(aggregate_count),
    ', unreadA=', (
        SELECT COUNT(*)
        FROM community_notification_recipient
        WHERE recipient_user_id = $userAId
          AND status = 'UNREAD'
    )
)
FROM community_notification
WHERE id IN (
    SELECT notification_id
    FROM community_notification_recipient
    WHERE recipient_user_id IN (
        $userAId, $userBId, $userCId, $userDId
    )
);
"@
    $schemaEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'columns=', (
        SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'community_notification'
          AND column_name IN (
              'category', 'importance',
              'aggregate_count', 'last_activity_at'
          )
    ),
    ', indexes=', (
        SELECT COUNT(DISTINCT index_name)
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'community_notification'
          AND index_name IN (
              'uk_notification_deduplication',
              'idx_notification_category_activity'
          )
    ),
    ', checks=', (
        SELECT COUNT(*)
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'community_notification'
          AND constraint_name IN (
              'chk_notification_category',
              'chk_notification_importance',
              'chk_notification_aggregate_count'
          )
    )
);
"@

    [ordered]@{
        backend = "UP"
        anonymousInbox = 401
        followAggregation = "1 record / 3 actors"
        allFollowerUpdates = "2 aggregated / HIGH"
        importantFollowerUpdates = "important only / HIGH"
        mutedFollowerUpdates = 0
        directNotifications = "LIKE/FAVORITE/COMMENT/REPLY/MENTION/REPOST"
        selfNotification = "suppressed"
        idempotentLikeAggregate = 1
        relikeAggregateAndUnread = "2 / UNREAD"
        authorUnreadByCategory = "FOLLOW1/INTERACTION3/COMMENT1"
        readReplay = "false/true"
        readAll = "3 then 2 / final 0"
        cancellationNotifications = 0
        inaccessibleTargetRedaction = "sender/content/id/path hidden"
        databaseEvidence = ($databaseEvidence -join "")
        schemaEvidence = ($schemaEvidence -join "")
    } | ConvertTo-Json -Depth 10
}
catch {
    $failure = $_
    Write-Host "E2E_FAILED: $($_.Exception.Message)"
}
finally {
    try {
        $env:MYSQL_PWD = $DatabasePassword
        $knownTargetIds = @(
            $blogAId,
            $textId,
            $projectId,
            $repostId,
            $rootCommentId,
            $replyCommentId
        ) | Where-Object { $_ }
        $targetList = if ($knownTargetIds.Count -gt 0) {
            $knownTargetIds -join ","
        }
        else {
            "0"
        }
        $cleanupSql = @"
SET @user_a := (
    SELECT id FROM community_user
    WHERE username = '$usernameA' LIMIT 1
);
SET @user_b := (
    SELECT id FROM community_user
    WHERE username = '$usernameB' LIMIT 1
);
SET @user_c := (
    SELECT id FROM community_user
    WHERE username = '$usernameC' LIMIT 1
);
SET @user_d := (
    SELECT id FROM community_user
    WHERE username = '$usernameD' LIMIT 1
);
SET @blog_a := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_a AND blog_type = 'PERSONAL' LIMIT 1
);
SET @blog_b := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_b AND blog_type = 'PERSONAL' LIMIT 1
);
SET @blog_c := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_c AND blog_type = 'PERSONAL' LIMIT 1
);
SET @blog_d := (
    SELECT id FROM blog
    WHERE owner_user_id = @user_d AND blog_type = 'PERSONAL' LIMIT 1
);

DELETE FROM community_notification
WHERE sender_user_id IN (@user_a, @user_b, @user_c, @user_d)
   OR target_id IN ($targetList)
   OR id IN (
        SELECT notification_id
        FROM community_notification_recipient
        WHERE recipient_user_id IN (
            @user_a, @user_b, @user_c, @user_d
        )
   );
DELETE FROM community_comment_moderation_event
WHERE comment_id IN (
    SELECT id FROM community_comment
    WHERE target_type = 'MOMENT'
      AND target_id IN (
          SELECT id FROM community_moment
          WHERE actor_user_id IN (
              @user_a, @user_b, @user_c, @user_d
          )
      )
);
DELETE FROM community_content_like
WHERE user_id IN (@user_a, @user_b, @user_c, @user_d)
   OR (
        target_type = 'MOMENT'
        AND target_id IN (
            SELECT id FROM community_moment
            WHERE actor_user_id IN (
                @user_a, @user_b, @user_c, @user_d
            )
        )
   )
   OR (
        target_type = 'COMMENT'
        AND target_id IN (
            SELECT id FROM community_comment
            WHERE author_user_id IN (
                @user_a, @user_b, @user_c, @user_d
            )
        )
   );
DELETE FROM community_comment
WHERE author_user_id IN (@user_a, @user_b, @user_c, @user_d)
  AND root_comment_id IS NOT NULL;
DELETE FROM community_comment
WHERE author_user_id IN (@user_a, @user_b, @user_c, @user_d);
DELETE FROM favorite_folder_item
WHERE favorite_item_id IN (
    SELECT id FROM favorite_item
    WHERE owner_user_id IN (
        @user_a, @user_b, @user_c, @user_d
    )
);
DELETE FROM favorite_item
WHERE owner_user_id IN (@user_a, @user_b, @user_c, @user_d);
DELETE FROM community_moment
WHERE actor_user_id IN (@user_a, @user_b, @user_c, @user_d);
DELETE FROM community_follow
WHERE follower_user_id IN (@user_a, @user_b, @user_c, @user_d)
   OR (
        target_type = 'BLOG'
        AND target_id IN (@blog_a, @blog_b, @blog_c, @blog_d)
   );
DELETE FROM favorite_folder_item
WHERE folder_id IN (
    SELECT id FROM favorite_folder
    WHERE owner_user_id IN (
        @user_a, @user_b, @user_c, @user_d
    )
);
DELETE FROM favorite_folder
WHERE owner_user_id IN (@user_a, @user_b, @user_c, @user_d);
DELETE FROM blog_category
WHERE blog_id IN (@blog_a, @blog_b, @blog_c, @blog_d);
UPDATE community_user
SET personal_blog_id = NULL
WHERE id IN (@user_a, @user_b, @user_c, @user_d);
DELETE FROM blog
WHERE id IN (@blog_a, @blog_b, @blog_c, @blog_d);
DELETE FROM community_user
WHERE id IN (@user_a, @user_b, @user_c, @user_d);
"@
        $null = Invoke-MySql -Sql $cleanupSql
        $cleanupEvidence = Invoke-MySql -Sql @"
SELECT CONCAT(
    'users=', (
        SELECT COUNT(*) FROM community_user
        WHERE username IN (
            '$usernameA', '$usernameB',
            '$usernameC', '$usernameD'
        )
    ),
    ', moments=', (
        SELECT COUNT(*) FROM community_moment
        WHERE id IN ($targetList)
    ),
    ', notifications=', (
        SELECT COUNT(*) FROM community_notification
        WHERE target_id IN ($targetList)
    ),
    ', comments=', (
        SELECT COUNT(*) FROM community_comment
        WHERE id IN ($targetList)
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
