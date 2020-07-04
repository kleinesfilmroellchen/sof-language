// This package-info is a hack to make the module-info always find the test
// resource folder, even if the test class files aren't properly included with
// the module. This bypasses the check for package existance for the
// "opens klfr.sof.test.source" module command, which is required to access all
// non-class resources in the folder. However, the actual resources themselves,
// as stated, are not in this folder, but in the parallel folder of the test
// binary directory tree. Java -_-
package klfr.sof.test.source;