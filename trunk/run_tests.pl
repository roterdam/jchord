#!/usr/bin/perl -w

use strict;
use Cwd 'abs_path';

my $testcases_dir = abs_path('tests/');
my @testcase_dirs = glob("$testcases_dir/*");

sub sort_and_diff {
	my ($cmds, $file1, $file2) = @_;
	push @$cmds, "sort $file1 -o $file1";
	push @$cmds, "sort $file2 -o $file2";
	push @$cmds, "diff $file1 $file2";
}

my $basic_doms_str = "M,F,V,P,I,E,H,L,R,Z";
my $basic_rels_str = "aryElemE,cha,clinitM,EF,EV,HT,IinvkArg0,IinvkArg,IinvkRet,initM,instF,instFldE,instI,instM,LE,LI,LL,ME,MgetInstFldInst,MgetStatFldInst,MH,MI,ML,MmethArg,MmethRet,MobjValAsgnInst,MobjVarAsgnInst,MPhead,MP,MPtail,MputInstFldInst,MputStatFldInst,MV,PE,PgetInstFldInst,PgetStatFldInst,PI,PL,PobjValAsgnInst,PobjVarAsgnInst,PP,PputInstFldInst,PputStatFldInst,privateM,specIM,statF,statFldE,statIM,statM,syncLM,syncLV,syncM,thisMV,virtIM,VT,writeE";
my $basic_doms_and_rels_str = "$basic_doms_str,$basic_rels_str";
my @basic_doms_ary = split(/,/, $basic_doms_str);
my @basic_rels_ary = split(/,/, $basic_rels_str);

for my $dir (@testcase_dirs) {
	my @cmds;
	push @cmds, "ant -f $dir/build.xml clean";
	push @cmds, "ant -f $dir/build.xml compile";
	push @cmds, "ant -Dchord.work.dir=$dir -Dchord.build.scope=true -Dchord.scope.exclude=\"java.,javax.,sun.,joeq.,jwutil.,com.sun.,com.ibm.,org.apache.harmony.\" run";
	sort_and_diff(\@cmds, "$dir/chord_output/classes.txt", "$dir/correct_output/classes.txt");
	sort_and_diff(\@cmds, "$dir/chord_output/methods.txt", "$dir/correct_output/methods.txt");
	push @cmds, "ant -Dchord.work.dir=$dir -Dchord.reuse.scope=true -Dchord.analyses=$basic_doms_and_rels_str -Dchord.print.rels=$basic_rels_str run";
	for my $dom (@basic_doms_ary) {
		sort_and_diff(\@cmds, "$dir/chord_output/bddbddb/$dom.map", "$dir/correct_output/$dom.map");
	}
	for my $rel (@basic_rels_ary) {
		sort_and_diff(\@cmds, "$dir/chord_output/$rel.txt", "$dir/correct_output/$rel.txt");
	}
	#push @cmds, "ant -Dchord.work.dir=$dir -Dchord.reuse.scope=true -Dchord.analyses=cipa-0cfa-dlog -Dchord.print.rels=FH,VH,HFH,IM run";
	#sort_and_diff(\@cmds, "$dir/chord_output/FH.txt" , "$dir/chord_output/FH.txt" );
	#sort_and_diff(\@cmds, "$dir/chord_output/VH.txt" , "$dir/chord_output/VH.txt" );
	#sort_and_diff(\@cmds, "$dir/chord_output/HFH.txt", "$dir/chord_output/HFH.txt");
	#sort_and_diff(\@cmds, "$dir/chord_output/IM.txt" , "$dir/chord_output/IM.txt" );
	print "========== TEST CASE: $dir\n";
    for my $cmd (@cmds) {
		print "=== $cmd\n";
		if (system($cmd) != 0) {
			print "*** ERROR: '$cmd' failed: $?\n";
			next;
		}
	}
}

