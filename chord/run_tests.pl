#!/usr/bin/perl -w

use strict;
use Cwd 'abs_path';

my $testcases_dir = abs_path('tests/');
my @testcase_dirs = glob("$testcases_dir/*");

for my $testcase_dir (@testcase_dirs) {
	print "========== TEST CASE: $testcase_dir\n";
	my $compile_cmd = "ant -f $testcase_dir/build.xml compile";
	if (system($compile_cmd) != 0) {
		print "*** '$compile_cmd' failed: $?\n";
		next;
	}
	my $run_cmd = "ant -Dchord.work.dir=$testcase_dir -Dchord.scope.kind=dynamic -Dchord.analyses=dynamic-thresc-java run";
	if (system($run_cmd) != 0) {
		print "*** '$run_cmd' failed: $?\n";
		next;
	}
}

