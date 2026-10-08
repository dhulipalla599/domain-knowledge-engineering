# Setup (browser only, no API key)

You need a **Claude Pro or Max** plan (routines aren't on the Free plan). Nothing is installed on your computer.

## 1. Create the repository (as dhulipalla599)
1. github.com → **+** → **New repository** → name `domain-knowledge-engineering`, **Public**, nothing pre-checked → **Create repository**.

## 2. Upload the files
1. Click **uploading an existing file**.
2. Drag everything *inside* the unzipped folder onto the page, including `.github` and `.gitignore` (Mac: `Cmd+Shift+.` shows hidden files; Windows: View → Show → Hidden items).
3. **Commit directly to main**.
4. Check `.github/workflows/` has `auto-merge.yml`, `deploy-docs.yml`, `open-pr.yml`. If not, create each with **Add file → Create new file** (type the full path as the name) and paste the contents.

## 3. Add your friend
**Settings → Collaborators → Add people → `naveenks720`**. They must accept the email invite.

## 4. Repository settings
- **Settings → General → Pull Requests:** check **Automatically delete head branches**.
- **Settings → Actions → General → Workflow permissions:** **Read and write permissions** + **Allow GitHub Actions to create and approve pull requests** → **Save**.
- **Settings → Pages → Source:** **GitHub Actions**.
- **Settings → Rules → Rulesets → New branch ruleset:** name `protect-main`, **Active**, target **Include default branch**, keep *Restrict deletions* and *Block force pushes*, check **Require a pull request before merging** with **Required approvals = 0**, Code Owners review **off** → **Create**.

## 5. Connect Claude to GitHub
1. Open https://claude.ai/code and sign in with your Pro/Max account.
2. Follow the prompt to connect GitHub and install the **Claude** GitHub App. When asked which repositories, choose **Only select repositories → domain-knowledge-engineering**.

## 6. Create the routine
1. Open https://claude.ai/code/routines → **New routine**.
2. **Name:** `Daily domain topic`.
3. **Instructions:** paste the text between the lines in `ROUTINE_PROMPT.md` (it tells Claude to follow `ROUTINE.md`, so later changes need no routine edit). In the model selector, Sonnet is a good choice (it uses less of your plan than Opus).
4. **Repository:** `dhulipalla599/domain-knowledge-engineering`.
5. **Environment:** **Default** (its trusted network allows installing Python packages).
6. **Trigger:** **Schedule → Daily → 11:30 AM** (entered in your local time; daylight saving is handled).
7. **Connectors:** remove all of them; this routine doesn't need any.
8. **Create**.

## 7. Test
1. GitHub → **Actions → Deploy website → Run workflow**. The site appears at `https://dhulipalla599.github.io/domain-knowledge-engineering/`.
2. On the routine page, click **Run now** and open the session to watch it (about 5-10 minutes).
3. When it pushes the branch, GitHub **Actions → Open topic PR** runs and a PR appears under **Pull requests**.
4. Today's scheduled run will then skip, because one topic was already written today.

## Daily routine
- The PR is assigned to one of you, alternating each day.
- Read it, add **Practitioner Notes**, **Approve**, then **Merge** (Create a merge commit).
- Missed it? The bot merges after 24 hours. Add the `hold` label or request changes to stop that.
- Closing a PR without merging? Click **Delete branch** too, or that topic stays reserved.

## Alternating contributors (on by default)
One routine is enough. Each day's commit is credited to the next person in `commit_authors:` in `config.yaml` (dhulipalla599, naveenks720, dhulipalla599, ...), and the PR is assigned to the other person to approve and merge. Merge with **Create a merge commit** (not squash), so the original author stays on the commit and it counts on that person's contribution graph.

## Optional: alternate the writing too
Your friend can create the same routine on their own Pro/Max account (steps 5-6, using `--me naveenks720` in the prompt). Then set `authors:` in `config.yaml` to both names. Each routine writes on alternate days and the other person reviews.

## Troubleshooting
| Symptom | Fix |
|---|---|
| Routine says "is not listed under authors" | Add that username under `authors:` in `config.yaml` |
| Routine can't clone or push | Re-check step 5 (GitHub App has access to the repo) |
| `pip install` blocked in the routine | Edit the routine's environment → Network access → Custom, include the default package list |
| No PR after the routine pushed | Check **Actions → Open topic PR** log; re-check step 4 workflow permissions |
| PR check is red | The PR comment lists what failed (usually a diagram); edit the file in the PR |
| Topic skipped as "waiting for review" | 3 topic branches are open; merge or close (and delete) them |
| Routine says its push to `feature/...` was rejected | It pushes to `claude/feature/...` instead, which also opens a PR. To stop that, make sure no ruleset protects `feature/*` branches |
| PR check is red because the example failed to build | Open the PR comment for the Maven output; fix the code in the PR, or comment `@claude` in a session to fix it |
| Run didn't happen | Check remaining daily runs and plan usage at https://claude.ai/code/routines |
