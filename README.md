# Redex Plugin

A Gradle plugin that allows you to use Facebook's Redex tool as part of your
build process

## Usage
Before you can use this plugin, you must install the Redex tool. Instructions
for installing Redex can be found at https://github.com/facebook/redex

Once Redex is installed, add the following to your `build.gradle`:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.0.0'
        classpath 'au.com.timmutton:redex:1.4.0'
    }
}

apply plugin: 'com.android.application'
apply plugin: 'redex'

// Optional: configure redex command line arguments
redex {
    // These are example arguments, fill them with your specific arguments
    configFile = 'redex.config'
    // `passes` is shorthand for a pass list instead of a config file
    // passes = ['ReBindRefsPass', ..., 'ShortenSrcStringsPass']
    proguardConfigFiles = ['common_proguard.pro', 'my_app_proguard.pro']
    proguardMapFile = 'proguard_map.txt'
    keepFile = 'keep.txt'
    jarFiles = ['lib1.jar', 'lib2.jar']
    otherArgs = '' // any other command line options to `redex`
    // see `redex --help` for details on these arguments
}

```
If you do not set the passes or the config file, the default set of passes will
be run. Sometimes you may not want to run all optimisation passes, for example
some appear to break when optimising kotlin code.

If you specified a signing configuration for the given build type, this plugin
will use that configuration to re-sign the application (Redex normally un-signs
the apk).

## License
The MIT License (MIT)

Copyright (c) 2016 Tim Mutton

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

