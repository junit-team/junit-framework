import module java.base;

void main(String[] args) {
    var gitDir = Path.of(args[0]);

    var reader = new BufferedReader(new InputStreamReader(System.in));
    var paths = reader.lines()
            .map(String::strip)
            .map(line -> line.split(" ", 2)[1])
            .map(Path::of)
            .toList();

    var numChangesByDir = paths.stream()
            .filter(path -> path.getNameCount() > 1)
            .collect(Collectors.groupingBy(path -> path.getName(0), Collectors.counting()));

    paths.stream()
            .filter(path -> path.getFileName().toString().endsWith(".pdf"))
            .filter(pdfFile -> numChangesByDir.get(pdfFile.getName(0)) == 1)
            .forEach(unchangedPdfFile -> {
                IO.println("echo Restoring %s...".formatted(unchangedPdfFile));
                IO.println("git -C %s restore %s".formatted(gitDir, unchangedPdfFile));
            });
}
