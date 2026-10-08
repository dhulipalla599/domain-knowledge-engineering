# Daily routine steps

The Claude routine reads this file on every run and follows it exactly.
`<ME>` is the GitHub username given in the routine's prompt.

1. Run `pip install -q -r scripts/requirements-routine.txt`.
2. Run `python scripts/next_topic.py --me <ME>` and read the JSON it prints.
   If "action" is "skip", reply with the reason in one line and stop. Do not create branches, files or pull requests.
3. Read the brief file named in "brief" (`.routine/brief.md`). It has the order of work, the writing rules,
   the exact page skeleton and every required section.
4. **Runnable example.** A Spring Boot skeleton already exists in "example_path". Implement it as the brief describes.
   Then run "build_command" inside "example_path" and fix the code until the build and all tests pass.
   If Java or Maven is not installed in this environment, say so in your final line and continue;
   GitHub Actions builds and tests the example when the branch is pushed.
5. **Page.** Write the complete page to "path", following the brief exactly. Code excerpts and the
   use case / UML diagrams must match the example you just wrote. Keep Mermaid syntax simple and valid
   (quote labels that contain punctuation).
6. Run `python scripts/check_page.py <path> --build` (drop `--build` only if Maven is missing).
   Fix every problem it reports and run it again until it passes.
7. Commit and push:
   - `git checkout -b <branch>` using "branch" from the JSON.
   - `git add` exactly the paths in "files_to_commit" (never `.routine/` or `target/`).
   - Commit as the author from the JSON:
     `git -c user.name="<git_author_name>" -c user.email="<git_author_email>" commit -m "<commit_message>"`
     (if those two fields are empty, commit normally).
   - `git push -u origin <branch>`. If that push is rejected, push the same commit to "fallback_branch" instead:
     `git push -u origin HEAD:<fallback_branch>`.
8. Do not open a pull request, do not change any other file, and never push to main.
   A GitHub workflow opens the pull request and runs the checks automatically.
9. Finish with one line: the topic, the branch you pushed, and whether the example's tests passed.
