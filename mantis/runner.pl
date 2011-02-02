#!/usr/bin/perl

use strict;
use Getopt::Long;
use Cwd;

my $cur_dir = getcwd;
my $main_class;
my $program_dir;
my $out_dir;
my $num_runs = 5;
my $chord_ext_scope_exclude="java.,javax.";

my $result = GetOptions(
	'main_class=s' => \$main_class,
    'program_dir=s' => \$program_dir,
    'out_dir=s' => \$out_dir,
    'num_runs=i' => \$num_runs,
	'chord_ext_scope_exclude=s' => \$chord_ext_scope_exclude
);

if (!$main_class) {
	die "Expected -main_class=<main class name>\n";
}
if (!$program_dir) {
	die "Expected -program_dir=<program dir name>\n";
}
if (!$out_dir) {
	die "Expected -out_dir=<output dir name>\n";
}

my $do_instrument = 1;
my $run_original = 1;
my $run_instrumented = 1;
my $do_aggregate = 1;
my $build_model = 1;

my $clean_classpath = "$program_dir/classes";
my $instr_classpath = "$program_dir/chord_output/user_classes";

### instrument given program

if ($do_instrument) {
    print "*** Instrumenting program\n";
    my $cmd = "ant -f ./build.xml " .
		"-Dchord.ext.scope.exclude=$chord_ext_scope_exclude " .
		"-Dchord.mantis.out.dir=$out_dir " .
		"-Dchord.work.dir=$program_dir " .
		"-Dchord.run.analyses=mantis-java " .
		"run";
    system($cmd) == 0 or die "Failed: $cmd: $!\n";
}

### run the original uninstrumented program on given inputs to get execution time

if ($run_original) {
    print "*** Running original uninstrumented program\n";

	for (my $i = 0; $i < $num_runs; $i++) {
		print "Running program on input in directory: $i\n";
		my $dir = "$out_dir/$i";
		if (-d $dir) {
			chdir $dir;
			my $cmd = "/usr/bin/time -f %e --output=exectime.txt " .
				"java -cp $clean_classpath $main_class input.txt " .
				"> out1.txt 2> err1.txt";
			print "Running command: $cmd ... ";
			if (system($cmd) == 0) {
				print "SUCCESS\n";
			} else {
				print "FAILURE\n";
			}
		} else {
			print "Directory $dir does not exist; skipping\n";
		}
	}
    chdir $cur_dir;
}

### run the instrumented program on given inputs to get feature profiles

if ($run_instrumented) {
    print "*** Running instrumented program\n";

	for (my $i = 0; $i < $num_runs; $i++) {
		print "Running program on input in directory: $i\n";
		my $dir = "$out_dir/$i";
		if (-d $dir) {
			chdir $dir;
			my $cmd = "java -cp $instr_classpath $main_class input.txt " .
				"> out2.txt 2> err2.txt";
			print "Running command: $cmd ... ";
			if (system($cmd) == 0) {
				print "SUCCESS\n";
			} else {
				print "FAILURE\n";
			}
		} else {
			print "Directory $dir does not exist; skipping\n";
		}
	}
    chdir $cur_dir;
}

if ($do_aggregate) {
    print "*** Aggregating profile data\n";

    my $cmd = "java -cp $cur_dir/classes " .
		"-Dchord.mantis.out.dir=$out_dir -Dchord.mantis.num.runs=$num_runs " .
		"chord.analyses.mantis.PostProcessor";
    system($cmd) == 0 or die "Failed: $cmd: $!\n";

    my $global_exectime_file = "$out_dir/exectime.txt";
    open GLOBAL_FILE, ">$global_exectime_file" or die "Failed to create file: $global_exectime_file: $!\n";
    for (my $i = 0; $i < $num_runs; $i++) {
        my $local_exectime_file = "$out_dir/$i/exectime.txt";
        open LOCAL_FILE, "<$local_exectime_file" or die "Failed to read file: $local_exectime_file: $!\n";
        my $line = <LOCAL_FILE>;
        chomp $line;
        print GLOBAL_FILE "$line\n";
        close LOCAL_FILE;
    }
    close GLOBAL_FILE;
}

if ($build_model) {
    print "*** Building performance model\n";

    my $cmd = "octave -qf -p $cur_dir/ml/common $cur_dir/ml/stable/foba_poly_model_init.m $out_dir 1";
    print "Running command: $cmd ...\n";
    system($cmd) == 0 or die "Failed: $cmd: $!\n";

    my $cmd = "octave -qf -p $cur_dir/ml/common $cur_dir/ml/stable/foba_poly_model.m $out_dir 2";
    print "Running command: $cmd ...\n";
    system($cmd) == 0 or die "Failed: $cmd: $!\n";

#   // run common part of slicer once and for all
#   while (true) {
#       // read currently_chosen_features.txt [estimation error, [list of chosen features]]
#       // if estimation error is above a threshold then break
#       // slice on each of them
#       // execute each slice on some fraction of inputs; if cost exceeds threshold, then
#       // append feature to costly_features.txt
#       // if all features are cheap then break
#   }
}
