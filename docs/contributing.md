# How We Work

New topics are written by a Claude routine every day at 11:30 AM Eastern. A GitHub workflow turns each one into a pull request and checks it.

## Daily review (maintainers)

1. Open the PR assigned to you (reviewers alternate each day).
2. Check the content against the checklist in the PR description.
3. Add something real to **Practitioner Notes**: open *Files changed*, click the `...` menu on the file, choose *Edit file*, and commit to the same branch.
4. **Approve**, then **Merge pull request** using *Create a merge commit*.

If nobody acts within 24 hours the bot merges the PR automatically. To stop that, add the `hold` label or submit a review that requests changes.

## Changing the content

| What you want | Edit |
|---|---|
| Different stack (e.g. Node.js, Azure) | `stack:` in `config.yaml` |
| Different model | the model selector in the routine at claude.ai/code/routines |
| Add, remove or reorder sections | the `## ` headings in `prompts/` and `stages:` in `config.yaml` |
| New domains or topics | `domains.yaml` |
| Rotate across domains vs. finish one at a time | `selection:` in `config.yaml` |

Changes apply from the next daily run. Pages already published are not regenerated.
