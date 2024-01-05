# DocTeX
### A tool for creating LaTeX Documentation documents from JavaDoc Comments


DocTeX uses [Spoon](https://github.com/INRIA/spoon) to parse the provided source code and javadoc and creates a $\LaTeX$ Document from it.

## Usage
### CLI
Clone the repository, build it with `./gradlew[.bat] run` and then run it with `java -jar build/libs/DocTeX-1.0-all.jar`.
The `-h` Option displays all possible and necessary options:
```
Usage: doctex [<options>] <sourcedir> <rootpackage>

Options:
  --output=<path>          (default: ./documentation.tex)
  --minimum-visibility=(PRIVATE|PROTECTED|PACKAGE|PUBLIC)
                           (default: PROTECTED)
  --inherit / --noInherit  Whether overriding methods that have no JavaDoc of
                           their own should inherit the documentation of the
                           method they are overriding (default: enabled)
  --classpath=<path>       The classpath of your application, should be a
                           folder containing .class files or a jar. Improves
                           resolution of external classes.
  -h, --help               Show this message and exit

Arguments:
  <sourcedir>    The directory containing all source files the documentation
                 should be generated for
  <rootpackage>  The package containing all subpackages and classes the
                 documentation should be generated for
```
### Ignoring Classes and Members
If you don't want to include a specific constructor/type/method/field you can annotate it with `@DoctexIgnore`
The necessary dependency for it is hosted [here](https://mvn.packages.mr-pine.de/#/releases/de/mr-pine/doctex/annotations) ([Adding the repository](https://mvn.packages.mr-pine.de/#/releases))
