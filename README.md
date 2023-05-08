# jcl2groovy

Convert JCL to Groovy for use with IBM Dependency Based Build (DBB). Currently only a limited subset of JCL statements is supported.

## Installation

Install `leiningen` , clone this repo and run `lein uberjar` to build a `.jar`. Alternatively download a `.jar` from the release section.

## Usage

The input JCL has to be provided as a file. Run the following to generate the corresponding Groovy code:

    $ java -jar jcl2groovy-0.1.0-standalone.jar [options] input.jcl

## Options

```
jcl2groovy [options] input.jcl
  -I, --include PATH  []  INCLUDE path
  -g, --debug             dump debug information
  -t, --todo              print generated @TODO comments
  -h, --help              print this message
```

## Examples

...

### Bugs

- 

## License

Copyright © 2022 FIXME

