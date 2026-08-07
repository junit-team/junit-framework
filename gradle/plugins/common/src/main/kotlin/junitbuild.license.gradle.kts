extensions.add(
	"license",
	License(
		name = "Eclipse Public License v2.0",
		url = uri("https://www.eclipse.org/legal/epl-v20.html"),
		headerFile = layout.settingsDirectory.file("gradle/config/spotless/eclipse-public-license-2.0.java"),
	),
)
