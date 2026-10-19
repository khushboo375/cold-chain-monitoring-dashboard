
# Branching Strategy

## Main branch
- `main`: Contains the stable, reviewed version of the project.

## Feature branches
- `feature/<short-description>`
- Example: `feature/dashboard-search`

## Bug-fix branches
- `fix/<short-description>`
- Example: `fix/temperature-validation`

## DevOps branches
- `devops/<short-description>`
- Example: `devops/jenkins-pipeline`

## Documentation branches
- `docs/<short-description>`
- Example: `docs/setup-guide`

## Commit conventions
Use clear, meaningful commit messages:
- `feat: add cold-chain reading form`
- `fix: correct temperature status calculation`
- `docs: update project setup instructions`
- `test: add dashboard integration test`
- `ci: add Jenkins build pipeline`
- `build: update Maven dependencies`

## Collaboration rules
1. Do not commit unfinished changes directly to `main`.
2. Create a branch for each feature, fix or DevOps change.
3. Keep commits focused and descriptive.
4. Open a pull request for review before merging.
5. Run relevant tests before merging.
6. Delete merged feature branches when they are no longer needed.
