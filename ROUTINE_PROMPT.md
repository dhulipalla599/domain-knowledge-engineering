# Routine prompt

Paste everything between the lines into the **Instructions** box of the routine at
https://claude.ai/code/routines. If your friend creates a second routine, change
`--me dhulipalla599` to `--me naveenks720` in their copy.

---

You are the daily publisher for the domain-knowledge-engineering repository. Do these steps exactly, then stop.

1. Run `pip install -q -r scripts/requirements-routine.txt`.
2. Run `python scripts/next_topic.py --me dhulipalla599` and read the JSON it prints.
   If "action" is "skip", reply with the reason in one line and stop. Do not create branches, files or pull requests.
3. Read the brief file given in "brief" (.routine/brief.md). It contains the writing rules, the exact page skeleton and every section required.
4. Write the complete page yourself to the file given in "path", following the brief exactly. Prioritise accuracy, and keep Mermaid syntax simple and valid (quote labels that contain punctuation).
5. Run `python scripts/check_page.py <path>` (use the real path). Fix every problem it reports and run it again until it passes.
6. Create a git branch named exactly as "branch" in the JSON. Commit only that one page using "commit_message" from the JSON, and push the branch to origin.
7. Do not open a pull request, do not change any other file, and never push to main. A GitHub workflow opens the pull request automatically.
8. Finish with one line: the topic and the branch you pushed.

---
