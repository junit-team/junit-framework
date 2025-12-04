module.exports = async ({ github, context }) => {
    const releaseVersion = process.env.RELEASE_VERSION;
    const requestBody = {
        owner: context.repo.owner,
        repo: context.repo.repo,
        tag_name: `r${releaseVersion}`,
        name: `JUnit ${releaseVersion}`,
        generate_release_notes: true,
        body: `JUnit ${releaseVersion} = Platform ${releaseVersion} + Jupiter ${releaseVersion} + Vintage ${releaseVersion}\n\nSee [Release Notes](https://docs.junit.org/${releaseVersion}/release-notes/).`,
        prerelease: releaseVersion.includes("-"),
    };
    console.log(requestBody);
    await github.rest.repos.createRelease(requestBody);
};
