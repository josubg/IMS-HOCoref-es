#!/usr/bin/octave -qf

%% Takes two tabular files with the scores (as produced by
%% make_tabular.sh and extract_doc_scores.pl).
%%
%% The p-value defaults to 0.05, change this in the file below:

PVALUE=0.05 %Set the p-value

arg_list=argv();
file1=arg_list{1}; %File 1
file2=arg_list{2}; %File 2
[DOCSf1,RECALLf1,PRECISIONf1,Ff1]=textread(file1,"%s %f %f %f","delimiter","\t");
[DOCSf2,RECALLf2,PRECISIONf2,Ff2]=textread(file2,"%s %f %f %f","delimiter","\t");

%Sanity check 1
if (length(DOCSf1) != length(DOCSf2))
  printf("Not same number of lines in files! -- %d vs %d",length(m1acc),length(m2acc));
  exit(1);
endif

%Sanity check 2
for i=1:length(DOCSf1),
  if (strcmp(DOCSf1(i),DOCSf2(i))==0)
    printf("Not same docs at index %d",i);
    exit(1);
  endif
end

%Do the math (also print means, could be interesting)
f1_recall_mean=mean(RECALLf1)
f2_recall_mean=mean(RECALLf2)
[recall_wcsgn_ranktest_pval,recall_wcsgn_ranktest_zstat] = wilcoxon_test(RECALLf1,RECALLf2)
printf("\n");
f1_precision_mean=mean(PRECISIONf1)
f2_precision_mean=mean(PRECISIONf2)
[prec_wcsgn_ranktest_pval,prec_wcsgn_ranktest_zstat] = wilcoxon_test(PRECISIONf1,PRECISIONf2)
printf("\n");
f1_f_mean=mean(Ff1)
f2_f_mean=mean(Ff2)
[f_wcsgn_ranktest_pval,f_wcsgn_ranktest_zstat] = wilcoxon_test(Ff1,Ff2)
printf("\n\n");

%Print easy-to-read results
printf("Recall significant at p<%f ?       %d\n",PVALUE,recall_wcsgn_ranktest_pval<PVALUE);
printf("Precision significant at p<%f ?    %d\n",PVALUE,prec_wcsgn_ranktest_pval<PVALUE);
printf("F significant at p<%f ?            %d\n",PVALUE,f_wcsgn_ranktest_pval<PVALUE);
printf("\n");
