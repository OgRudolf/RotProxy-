# Publishing Checklist

Use this checklist before making the repository public or uploading a release to Modrinth.

## Before GitHub

- Confirm `.gitignore` is present.
- Confirm `build/`, `.gradle/`, `.idea/`, `run/`, logs, and crash reports are not tracked.
- Do not commit any screenshots that show:
  - proxy hosts
  - proxy ports
  - proxy usernames
  - proxy passwords
  - local usernames or private file paths
- Do not commit local Minecraft config files.
- Review staged files before every push.

## Before Modrinth

- Upload only the built jar from `build/libs/`.
- Do not bundle personal configs.
- Do not include working paid proxy credentials in the description, gallery, or changelog.
- Mark the project as:
  - Fabric
  - Client-side
  - Minecraft `1.21.10`

## Account Safety

- Use two-factor authentication on GitHub and Modrinth.
- Use an alias or no-reply email if you do not want your main identity exposed publicly.
- Double-check profile bio, linked accounts, and social links before publishing.
