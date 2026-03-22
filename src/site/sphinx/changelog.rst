
************************
Changelog
************************


Version 1.4.0
=============================================================


  * **doc: Sphinx documentation**
    
    manticore-projects, 2026-03-22
  * **build: modernize gradle publish**
    
    manticore-projects, 2026-03-22
  * **fix: BGR/ABGR channel swap correctness and encoder performance**
    
    Andreas Reichel, 2026-03-22
  * **build: fix flags**
    
    manticore-projects, 2026-02-25

Version 1.3.1
=============================================================


  * **chore: relax the Maven test**
    
    Andreas Reichel, 2024-09-30
  * **style: fix format violations**
    
    Andreas Reichel, 2024-09-30
  * **chore: bump version of github actions**
    
    Andreas Reichel, 2024-09-30
  * **chore: bump version**
    
    Andreas Reichel, 2024-09-30
  * **style: reformat**
    
    Andreas Reichel, 2024-09-30

Version 1.3.0
=============================================================


  * **fix: buffer overflow in the Swap Bytes routine**
    
    Andreas Reichel, 2024-09-30
  * **build(deps): bump actions/download-artifact in /.github/workflows**
    
    dependabot[bot], 2024-09-03
  * **style: code formatting**
    
    Andreas Reichel, 2023-11-29
  * **build: add MacOS aarch64**
    
    Andreas Reichel, 2023-11-29
  * **build: add MacOS aarch64**
    
    Andreas Reichel, 2023-11-29
  * **doc: add simple Java calls to README**
    
    Andreas Reichel, 2023-11-29
  * **build: add MacOS aarch64**
    
    Andreas Reichel, 2023-11-29
  * **feat: use `release/stripped` binary (instead of `debug`)**
    
    Andreas Reichel, 2023-11-29
  * **feat: use `release/stripped` binary (instead of `debug`)**
    
    Andreas Reichel, 2023-11-28
  * **fix: Copy InputStream to file first, before ImageIO reading**
    
    Andreas Reichel, 2023-11-28
  * **doc: bump version references**
    
    Andreas Reichel, 2023-11-28
  * **style: spelling**
    
    Andreas Reichel, 2023-11-28
  * **build: fix the destination of the MacOS *.dylib**
    
    Andreas Reichel, 2023-11-28
  * **doc: rebuild the CHANGELOG.md**
    
    Andreas Reichel, 2023-11-26
  * **chore: add AGPL license file**
    
    Andreas Reichel, 2023-11-26
  * **style: exclude license headers from formatting**
    
    Andreas Reichel, 2023-11-26
  * **chore: release 1.2.0**
    
    Andreas Reichel, 2023-11-26

Version 1.2.0
=============================================================


  * **style: remove comment**
    
    Andreas Reichel, 2023-11-26
  * **style: apply license headers**
    
    Andreas Reichel, 2023-11-26
  * **style: add Q/A and Code Coverage tools, fix exceptions**
    
    Andreas Reichel, 2023-11-26
  * **test: add a multi-threaded test to check for memory leaks (just in case)**
    
    Andreas Reichel, 2023-11-25
  * **feat: add the BGR 3byte swap**
    
    Andreas Reichel, 2023-11-25
  * **doc: update the benchmark results**
    
    Andreas Reichel, 2023-11-24
  * **doc: update the benchmark results**
    
    Andreas Reichel, 2023-11-24
  * **feat: improve the benchmarks**
    
    Andreas Reichel, 2023-11-24
  * **fix: read resources from Class InputStream (instead System Resources)**
    
    Andreas Reichel, 2023-11-24
  * **feat: set the FPNGe Compression Level 1..5**
    
    Andreas Reichel, 2023-11-24
  * **doc: fine-tune the documentation**
    
    Andreas Reichel, 2023-11-23
  * **build: Release 1.1.0**
    
    Andreas Reichel, 2023-11-23

Version 1.1.0
=============================================================


  * **build: define `Encoder` as API dependency**
    
    Andreas Reichel, 2023-11-23
  * **doc: fine-tune the documentation**
    
    Andreas Reichel, 2023-11-23
  * **fix: include Macros for exporting function into DLL**
    
    Andreas Reichel, 2023-11-22
  * **fix: -mtune=generic**
    
    Andreas Reichel, 2023-11-22
  * **fix: -mtune=generic**
    
    Andreas Reichel, 2023-11-22
  * **fix: OS detection**
    
    Andreas Reichel, 2023-11-22
  * **fix: OS detection**
    
    Andreas Reichel, 2023-11-22
  * **fix: OS detection**
    
    Andreas Reichel, 2023-11-22
  * **ci: copy specific lib resources**
    
    Andreas Reichel, 2023-11-22
  * **ci: 1.0.2**
    
    Andreas Reichel, 2023-11-22
  * **fix: make the library resource distinct**
    
    Andreas Reichel, 2023-11-22
  * **ci: fix minimum required version after fixes**
    
    Andreas Reichel, 2023-11-21
  * **ci: fix minimum required version after fixes**
    
    Andreas Reichel, 2023-11-21
  * **ci: fix minimum required version after fixes**
    
    Andreas Reichel, 2023-11-21

Version 1.0.0
=============================================================


  * **ci: fix minimum required version after fixes**
    
    Andreas Reichel, 2023-11-21
  * **ci: fix minimum required version after fixes**
    
    Andreas Reichel, 2023-11-21
  * **doc: change log**
    
    Andreas Reichel, 2023-11-21
  * **build: remove redundant Gradle tasks**
    
    Andreas Reichel, 2023-11-21
  * **fix: call the correct class `FPNGEncoder` in the `maven-test`**
    
    Andreas Reichel, 2023-11-21
  * **fix: Create New Filesystem for ZIP, when reading from a JAR file**
    
    Andreas Reichel, 2023-11-21
  * **chore: 0.99.2**
    
    Andreas Reichel, 2023-11-21

Version 0.99.2
=============================================================


  * **chore: Sonatype insists in a JavaDoc**
    
    Andreas Reichel, 2023-11-21
  * **chore: Sonatype insists in a JavaDoc**
    
    Andreas Reichel, 2023-11-21
  * **fix: spelling of the Gradle task**
    
    Andreas Reichel, 2023-11-21
  * **chore: publish also `encoder-java` needed for `fpng-java` and `fpnge-java`**
    
    Andreas Reichel, 2023-11-21
  * **chore: 0.99.1**
    
    Andreas Reichel, 2023-11-21

Version 0.99.1
=============================================================


  * **chore: merge the GitHub actions, add the `maven-test` on multi OS**
    
    Andreas Reichel, 2023-11-21
  * **feat: add `maven-test` sub project**
    
    Andreas Reichel, 2023-11-21
  * **feat: introduce prefix='lib' for supporting the Windows OS**
    
    Andreas Reichel, 2023-11-21
  * **doc: write out the correct Sonatype Snapshot Repository**
    
    Andreas Reichel, 2023-11-21
  * **style: reformat**
    
    Andreas Reichel, 2023-11-20
  * **build: Pre-Release 0.99.0**
    
    Andreas Reichel, 2023-11-20

Version 0.99.0
=============================================================


  * **fix: FPNGE on Windows/MVCC**
    
    Andreas Reichel, 2023-11-20
  * **doc: explain the Maven Artifacts**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable FPNGE on windows**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable AVX function Windows Visual C**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable AVX function Windows Visual C**
    
    Andreas Reichel, 2023-11-20
  * **build: Disable AVX function Windows Visual C**
    
    Andreas Reichel, 2023-11-20
  * **build: Compiler flags for Windows Visual C**
    
    Andreas Reichel, 2023-11-20
  * **build: Compiler flags for Windows Visual C**
    
    Andreas Reichel, 2023-11-20
  * **build: Try Multi-OS build action**
    
    Andreas Reichel, 2023-11-20
  * **build: Try Multi-OS build action**
    
    Andreas Reichel, 2023-11-20
  * **build: Try Multi-OS build action**
    
    Andreas Reichel, 2023-11-20
  * **build: Try Multi-OS build action**
    
    Andreas Reichel, 2023-11-20
  * **build: Try Multi-OS build action**
    
    Andreas Reichel, 2023-11-20
  * **build: Update Github publish action**
    
    Andreas Reichel, 2023-11-20

Version 0.11
=============================================================


  * **build: Update Github publish action**
    
    Andreas Reichel, 2023-11-20
  * **build: Publish to Maven Repositories**
    
    Andreas Reichel, 2023-11-20
  * **Update build.gradle**
    
    manticore-projects, 2023-11-19
  * **Update gradle.properties**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle.properties**
    
    manticore-projects, 2023-11-19
  * **Update gradle.properties**
    
    manticore-projects, 2023-11-19
  * **Update gradle.properties**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **Create dependabot.yml**
    
    manticore-projects, 2023-11-19
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-19
  * **doc: Sphinx changelog**
    
    Andreas Reichel, 2023-11-19
  * **build: Multi OS JARs including the Native Libraries**
    
    Andreas Reichel, 2023-11-19
  * **doc: update the README with the latest benchmarks**
    
    Andreas Reichel, 2023-11-18
  * **doc: update the README with the latest benchmarks**
    
    Andreas Reichel, 2023-11-18
  * **doc: update the README with the latest benchmarks**
    
    Andreas Reichel, 2023-11-18
  * **perf: avoid expensive ABGR to RGBA translation**
    
    Andreas Reichel, 2023-11-18
  * **fix: dependency syntax**
    
    Andreas Reichel, 2023-11-15
  * **fix: write directly into the CharArray and avoid a `memcpy`**
    
    Andreas Reichel, 2023-11-15
  * **feat: add AVX optimized FPNGE**
    
    Andreas Reichel, 2023-11-15
  * **style: clean-up MD**
    
    Andreas Reichel, 2023-11-14
  * **doc: add badges**
    
    Andreas Reichel, 2023-11-14
  * **Create gradle.yml**
    
    manticore-projects, 2023-11-14
  * **doc: write some README**
    
    Andreas Reichel, 2023-11-14
  * **doc: write some README**
    
    Andreas Reichel, 2023-11-14
  * **Update gradle-publish.yml**
    
    manticore-projects, 2023-11-14
  * **Create README.md**
    
    manticore-projects, 2023-11-14
  * **Create gradle-publish.yml**
    
    manticore-projects, 2023-11-14
  * **build: standard Q/A and documentation tasks**
    
    Andreas Reichel, 2023-11-14

Version 0.9.0
=============================================================


  * **Initial Commit**
    
    Andreas Reichel, 2023-11-14

