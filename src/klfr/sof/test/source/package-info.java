// This package-info is a hack to make the module-info always find the test
// resource folder, even if the test class files aren't properly included with
// the module. This bypasses the check for package existance for the
// "opens klfr.sof.test.source" module command, which is required to access all
// non-class resources in the folder. However, the actual resources themselves,
// as stated, are not in this folder, but in the parallel folder of the test
// binary directory tree. Java -_-
package klfr.sof.test.source;

/*  
The SOF programming language interpreter.
Copyright (C) 2019-2020  kleinesfilmröllchen

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
