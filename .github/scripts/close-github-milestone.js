module.exports = async ({ github, context }) => {
    const releaseVersion = process.env.RELEASE_VERSION;
    const query = `
                query ($owner: String!, $repo: String!, $title: String!) {
                    repository(owner: $owner, name: $repo) {
                        milestones(first: 100, query: $title) {
                            nodes {
                                title
                                number
                                openIssueCount
                            }
                        }
                    }
                }
            `;
    const {repository} = await github.graphql(query, {
        owner: context.repo.owner,
        repo: context.repo.repo,
        title: releaseVersion
    });
    const [milestone] = repository.milestones.nodes.filter(it => it.title === releaseVersion);
    if (!milestone) {
        throw new Error(`Milestone "${releaseVersion}" not found`);
    }
    if (milestone.openIssueCount > 0) {
        throw new Error(`Milestone "${releaseVersion}" has ${milestone.openIssueCount} open issue(s)`);
    }
    const requestBody = {
        owner: context.repo.owner,
        repo: context.repo.repo,
        milestone_number: milestone.number,
        state: 'closed',
        due_on: new Date().toISOString()
    };
    console.log(requestBody);
    await github.rest.issues.updateMilestone(requestBody);
};
