#!/usr/bin/perl -w

use strict;
use Cwd 'abs_path';

my $testcases_dir = abs_path('tests/');
my @testcase_dirs = glob("$testcases_dir/*");

for my $dir (@testcase_dirs) {
	my @cmds;
	push @cmds, "ant -f $dir/build.xml clean";
	push @cmds, "ant -f $dir/build.xml compile";
	push @cmds, "ant -Dchord.work.dir=$dir -Dchord.scope.exclude=\"java.,javax.,sun.,joeq.,jwutil.,com.sun.\" run";
	push @cmds, "sort $dir/chord_output/classes.txt -o $dir/chord_output/classes.txt";
	push @cmds, "sort $dir/chord_output/methods.txt -o $dir/chord_output/methods.txt";
	push @cmds, "diff $dir/chord_output/classes.txt $dir/correct_output/classes.txt";
	push @cmds, "diff $dir/chord_output/methods.txt $dir/correct_output/methods.txt";

	push @cmds, "ant -Dchord.work.dir=$dir -Dchord.reuse.scope=true -Dchord.analyses=cipa-0cfa-dlog -Dchord.print.rels=FH,VH,HFH,IM run";
	push @cmds, "sort $dir/chord_output/FH.txt -o $dir/chord_output/FH.txt";
	push @cmds, "sort $dir/chord_output/VH.txt -o $dir/chord_output/VH.txt";
	push @cmds, "sort $dir/chord_output/HFH.txt -o $dir/chord_output/HFH.txt";
	push @cmds, "sort $dir/chord_output/IM.txt -o $dir/chord_output/IM.txt";
	print "========== TEST CASE: $dir\n";
    for my $cmd (@cmds) {
		print "=== $cmd\n";
		if (system($cmd) != 0) {
			print "*** ERROR: '$cmd' failed: $?\n";
			next;
		}
	}
}

