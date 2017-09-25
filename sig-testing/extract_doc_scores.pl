#!/usr/bin/perl

##
## Use it like this:
## scorer.pl <metric> <gold> <pred> | grep -v "^Repe" | tail -n +2 | head -n -6 | perl <this-script>
##
## The head and tail calls throws out rubbish at beginning and end.
## The grep for Repe throws out the Repe lines (which are a result of buggy gold)
##
## It then prints a sorted list of doc IDs which is tab separated and has the following values
## DOCNAME\tRECALL\tPRECISION\tF1
##
## Note that it doesn't parse the BLANC output properly (only use for muc, bcub, ceafm, ceafe)
##

use strict;
use warnings;

my ($key,$value);
my %lines;
while(($key,$value)=getLine()){
    last if(!defined($key));
#    print $line,"\n";
#    print $key,"\n";
#    print $value,"\n";
    not exists($lines{$key}) or die("key $key already present in line hash");
    $lines{$key}=$value;
}

foreach $key (sort keys %lines){
    print $key,"\t",$lines{$key},"\n";
}

print STDERR "Extracted ".scalar(keys %lines)." scores\n";

sub getLine {
    my $header;
    return undef if(!defined($header=<>));
    chomp $header;
    my $g1=<>;
    my $g2=<>;
    my $g3=<>;
    my $g4=<>;
    my $g5=<>;
    my $g6=<>;
    my $last=<>;
#    my $ret=$header . "\t";
    my $key=$header;
    my $values;
#    if($last=~/^Recall:.*(\d+(:?\.\d+)?)\%.*Precision:.*(\d+(:?\.\d+)?)\%.*F1: (\d+(:?\.\d+)?)\%$/){ #doesn't work. easier to split at %, keep the old to match the line
    if($last=~/^Recall.*F1: (\d+(\.\d+)?)\%$/){
	chomp $last;
	my $f=$1;
	my @a=split(/\%/,$last);
	my ($r,$p);
	if($a[0]=~/\d+(\.\d+)?$/){
	    $r=$&;
	} else {
	    die("error, a0: $a[0]");
	}
	if($a[1]=~/\d+(\.\d+)?$/){
	    $p=$&;
	} else {
	    die("error, a1: $a[1]");
	}
	if($a[2]=~/\d+(\.\d+)?$/){
	    $& eq $f or die("failed to match f1 and f1: $& -- $f");
	}else {
	    die("error, a2: $a[2]");
	}
	# print $r,"\n";
	# print $p,"\n";
	# print $f,"\n";
	# exit 0;
	$values=($r/100) . "\t" . ($p/100) . "\t" . ($f/100);
    } else {
	die("failed to parse $last");
    }
    my $dash=<>;
    $dash=~/^-+$/ or die("not dashes: $last");
#    return $ret;
    return ($key,$values);
}
