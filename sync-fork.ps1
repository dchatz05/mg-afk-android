<#
.SYNOPSIS
    Syncs the local fork with upstream and rebases your auto-buy feature branch.
.DESCRIPTION
    1. Fetches latest changes from upstream repository.
    2. Updates local main branch with upstream/main.
    3. Switches to feature/auto-buy and rebases on top of main.
#>

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Syncing Fork with Upstream & Rebasing..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Fetch from upstream
Write-Host "`n[1/4] Fetching upstream..." -ForegroundColor Yellow
git fetch upstream

# 2. Update main
Write-Host "`n[2/4] Updating local main branch..." -ForegroundColor Yellow
git checkout main
git merge upstream/main

# 3. Rebase feature branch
Write-Host "`n[3/4] Switching to feature/auto-buy and rebasing..." -ForegroundColor Yellow
git checkout feature/auto-buy
git rebase main

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host " Sync and rebase completed successfully!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
