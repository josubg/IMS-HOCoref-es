package ims.hotcoref.lang;


import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import ims.hotcoref.Options;
import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;
import ims.hotcoref.data.CFGTree.CFGNode;
import ims.hotcoref.data.CFGTree.NonTerminal;
import ims.hotcoref.data.CFGTree.Terminal;
import ims.hotcoref.features.FeatureSet;
import ims.hotcoref.features.enums.Gender;
import ims.hotcoref.features.enums.Num;
import ims.hotcoref.features.enums.SemanticClass;
import ims.hotcoref.headrules.HeadFinder;
import ims.hotcoref.headrules.HeadRules;
import ims.hotcoref.headrules.HeadRules.Direction;
import ims.hotcoref.headrules.HeadRules.Rule;
import ims.hotcoref.util.BergsmaLinLookup;
import ims.hotcoref.util.WordNetInterface;

public class Spanish extends Language {
    private static final long serialVersionUID = 1L;

    private final BergsmaLinLookup lookup;

    public Spanish() {
        try {
            lookup = new BergsmaLinLookup(Options.genderData);
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("!");
        }
    }

    @Override
    public FeatureSet getDefaultFeatureSet() {
        String[] names = {
                "MToHdForm+MFromHdForm",                            //1
//				"AnaphorDemonstrative",                             //2
                "MToSFForm",                                        //2'
                "CleverStringMatch",                                //3
                "DistanceBuckets+MToMTypeCoarse",                    //5
                "SameSpeaker+MFromPronounForm+MToPronounForm",        //6
                "MFromWholeSpanForm",                                //7
                "CFGSSPath+MToPronounForm",                            //8
                "MFromCFGParentNodeCategory",                        //9
                "MFromNodeCFGSubCat+Nested",                        //10
                "Genre+Nested",                                        //11
                "MFromSPrForm",                                        //12


//				"CFGDSFormPath",									//13


                "MFromWholeSpanForm+MToWholeSpanForm",                //14
                "MFromSFoPos",                                        //15
                "MToSFoPos+MFromSFForm",                            //16
                "MFromHdPos+MToPronounForm",                        //17
                "MToSFForm+MFromHdForm",                            //18
                "MFromParentNodeCFGSubCat",                            //19
                "MFromSPrForm+MToHdForm",                            //20
                "MFromParentNodeCFGSubCat+MToPronounForm",            //21
                "CleverStringMatch+MFromMTypeCoarse",                //22
                "Nested+MToMTypeCoarse",                            //23
                "DistanceBuckets+MToPronounForm",                    //24
                "CFGSSPosPath",                                        //25

//				"Alias",                                            //4
//				"Alias+MFromMTypeCoarse+MToMTypeCoarse",			//4'

                "Genre+MFromPronounForm+MToPronounForm",            //26
                "MFromHdINForm+MFromHdPos",                            //27
                "MFromSFoPos+MToPronounForm",                        //28
                "MFromGender+MToPronounForm",                        //29
                "MentionDistBuckets+MToMTypeCoarse",                //30
                "MFromParentNodeCFGSubCat+MentionDistBuckets+MToMTypeCoarse", // 31
                "CleverStringMatch+MFromMTypeCoarse+MFromHdForm+MToHdForm",   // 32
//				"MFromNamedEntity",											  // 33 // seems to work negatively for the treebased system
                "MToQuoted+MToPronounForm+MFromDominatingVerb",               // 34

        };
        return FeatureSet.getFromNameArray(names);
    }


    @Override
    public boolean cleverStringMatch(Span ant, Span ana) {
        String s1 = ana.getCleverString();
        String s2 = ant.getCleverString();
        return s1.length() > 0 && s1.equals(s2);
    }

    @Override
    public String computeCleverString(Span sp) {
        StringBuilder sb = new StringBuilder();
        for (int i = sp.start; i <= sp.end; ++i) {
            String pos = sp.s.tags[i].toUpperCase();
            if (sp.s.forms[i].equals("\"") ||
                    (pos.startsWith("D") && !(pos.startsWith("DT") || pos.startsWith("DP"))) ||
                    pos.equals("POS") || pos.startsWith("f"))
                continue;
            sb.append(sp.s.forms[i]).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean isAlias(Span ant, Span ana) {
        return AliasStuff.isAlias(ant, ana);
    }

    @Override
    public void computeAtomicSpanFeatures(Span s) {
        s.isProperName = isProperName(s);
        s.isPronoun = isPronoun(s);
        s.isDefinite = isDefinite(s);
        s.isDemonstrative = isDemonstrative(s);
        s.gender = lookupGender(s);
        s.number = lookupNumber(s);
        s.isQuoted = isQuoted(s);
        if (s.isPronoun) {
            s.semanticClass = pronounSemanticClassLookup(s.s.tags[s.hd].toUpperCase());
        } else {
            WordNetInterface wni = WordNetInterface.theInstance();
            if (wni != null)
                s.semanticClass = wni.lookupSemanticClass(s.s.forms[s.hd]);
        }
    }
//
    private boolean isQuoted(Span s) {
        boolean quoteBegin = false;
        boolean quoteEnd = false;

        for (int i = s.start - 1; i > 0; --i) {
            if (s.s.tags[i].equals("''"))
                return false;
            if (s.s.tags[i].equals("``")) {
                quoteBegin = true;
                break;
            }
        }
        if (!quoteBegin)
            return false;

        for (int i = s.end + 1; i < s.s.forms.length; ++i) {
            if (s.s.tags[i].equals("``"))
                return false;
            if (s.s.tags[i].equals("''")) {
                return true;
            }
        }
        return quoteEnd;
    }

    private SemanticClass pronounSemanticClassLookup(String pos) {
        if (pos.charAt(3) == 'F')
            return SemanticClass.Female;
        if (pos.charAt(3) == 'M')
            return SemanticClass.Male;
        if (pos.charAt(3) != 'N')
            return SemanticClass.Person;
        else
            return SemanticClass.Unknown;
    }

    private Num lookupNumber(Span s) {
        if (s.isPronoun) {
            String pos = s.s.tags[s.start].toUpperCase();
            if (pos.charAt(4) == 'S')
                return Num.Sin;
            if (pos.charAt(4) == 'P')
                return Num.Plu;
        }
        return lookup.lookupNum(s);
    }

    private static final Pattern MASC_TITLE_PATTERN = Pattern.compile("^(?:Sr\\.?|Señor)$");
    private static final Pattern FEM_TITLE_PATTERN = Pattern.compile("^(?:Srt?as?\\.?|Señora?s)$");

    private Gender getGenderChar(String pos, int charNumber){
        if (pos.length() < charNumber +1 ){
            System.err.println("POS Too short for gender extraction" + pos);
            return Gender.Unknown;
        }
        if (pos.charAt(charNumber) == 'M')
            return Gender.Masc;
        if (pos.charAt(charNumber) == 'F')
            return Gender.Fem;
        if (pos.charAt(charNumber) == 'N')
            return Gender.Neut;
        if (pos.charAt(charNumber) == 'C')
            return Gender.Unknown;
        return Gender.Unknown;
    }




    private Gender lookupGender(Span s) {

       if (s.isProperName) { //Might be a title of a person
           if (FEM_TITLE_PATTERN.matcher(s.s.forms[s.start]).matches())
               return Gender.Fem;
           if (MASC_TITLE_PATTERN.matcher(s.s.forms[s.start]).matches())
               return Gender.Masc;
       }


       String pos = s.s.tags[s.start].toUpperCase();
       switch (pos.charAt(0)){
           case 'A' :
                return  getGenderChar(pos,3);
           case 'W' :
           case 'Z' :
           case 'S' :
           case 'I' :
           case 'C' :
           case 'R' :
                return  Gender.Unknown;

           case 'D' :
                return  getGenderChar(pos,3);
           case 'N' :
               return  getGenderChar(pos,3);
           case 'V' :
               return  getGenderChar(pos,3);
           case 'P' :
               return  getGenderChar(pos,3);


        }
        //Otherwise we try the gender lookup

        return lookup.lookupGen(s);
    }

    private boolean isDemonstrative(Span s) {
        int len = s.end - s.start + 1;
        return (len != 1 && s.s.tags[s.start].startsWith("DD"));
    }

    private boolean isDefinite(Span s) {
        int len = s.end - s.start + 1;

        return (len != 1 && s.s.tags[s.start].startsWith("DA"));
    }

    private boolean isProperName(Span s) {
        int len = s.end - s.start + 1;
        if (len > 1) {
            for (int i = s.start; i < s.end; ++i) {
                if (!s.s.tags[i].startsWith("NP"))
                    return false;
            }
            return s.s.tags[s.end].startsWith("NP");
        } else {
            return s.s.tags[s.start].startsWith("NP");
        }
    }

    private boolean isPronoun(Span s) {
//        if (s.s.tags[s.hd].startsWith("PP") || s.s.tags[s.hd].startsWith("PX") || s.s.tags[s.hd].startsWith("DP"))
//        System.out.println("pronoun" + s.getSurfaceForm());
        int len = s.end - s.start + 1;
        return len <= 1 && (s.s.tags[s.hd].startsWith("PP") || s.s.tags[s.hd].startsWith("PX") || s.s.tags[s.hd].startsWith("DP"));
    }


    static class AliasStuff {

        public static boolean isAlias(Span ant, Span ana) {
            String antSFWTP = toSurfaceFormWithoutTrailingPossesives(ant);
            String anaSFWTP = toSurfaceFormWithoutTrailingPossesives(ana);
            if (ant.ne == null || ana.ne == null || !ant.ne.getLabel().equals(ana.ne.getLabel()))
                return false;
            String neLbl = ant.ne.getLabel();
            if (neLbl.equalsIgnoreCase("PERSON"))
                return comparePerson(antSFWTP.split(" "), anaSFWTP.split(" "));
            else if (neLbl.equalsIgnoreCase("ORG"))
                return compareOrg(ant, ana);
            else
                return false;

        }

        private static boolean comparePerson(String[] ant, String[] ana) {
            return ant[ant.length - 1].equals(ana[ana.length - 1]);
        }

        private static boolean compareOrg(Span ant, Span ana) {
            String antStr = toSurfaceFormWithoutTrailingPossesives(ant);
            String anaStr = toSurfaceFormWithoutTrailingPossesives(ana);
            return compareOrg(antStr, anaStr);
        }

        private static boolean compareOrg(String antStr, String anaStr) {
            if (antStr.replaceAll("\\.", "").equals(anaStr) ||
                    anaStr.replaceAll("\\.", " ").equals(antStr)) {
                return true;
            } else {
                if (antStr.length() > anaStr.length()) {
                    String[] acr = getAcronyms(antStr);
                    String s = loseInitialThe(anaStr);
                    return matchesAny(s, acr);
                } else {
                    String[] acr = getAcronyms(anaStr);
                    String s = loseInitialThe(antStr);
                    return matchesAny(s, acr);
                }
            }
        }


        public static String toSurfaceFormWithoutTrailingPossesives(Span s) {
            StringBuilder sb = new StringBuilder();
            for (int i = s.start; i <= s.end; ++i) {
                if (s.s.tags[i].equals("POS"))
                    continue;
                sb.append(s.s.forms[i]).append(" ");
            }
            return sb.toString().trim();
        }

        private static boolean matchesAny(String s, String[] acronyms) {
            for (String acro : acronyms) {
                if (s.equals(acro))
                    return true;
            }
            return false;
        }

        private static String loseInitialThe(String s) {
            return s.replaceFirst("^[Ee]l ", "")
                .replaceFirst("^[Ll]as? ", "")
                .replaceFirst("^[Ll]os ", "")
                .replaceFirst("^[Ll]a ", "");
        }

        private static String[] getAcronyms(String anaphorSurfaceForm) {
            String[] tokens = anaphorSurfaceForm.split(" ");
            StringBuilder a1 = new StringBuilder();
            StringBuilder a2 = new StringBuilder();
            StringBuilder a3 = new StringBuilder();
            for (String token : tokens) {

                if (!token.toLowerCase().matches("S\\.(A\\.|S\\.L\\.(L\\.)?(N\\.E\\.)?|coop|)?")) {
                    a1.append(token);
                    if (Character.isUpperCase(token.charAt(0))) {
                        a2.append(token.charAt(0));
                        a3.append(token.charAt(0)).append(".");
                    }
                }
            }
            return new String[]{a1.toString(), a2.toString(), a3.toString()};
        }
    }

    @Override
    public int findNonTerminalHead(Sentence s, CFGNode n) {
        return findSpanishCFGHead(s, n);
    }

    public static int findSpanishCFGHead(Sentence s, CFGNode n) {
        if (n == null)
            return -1;
        if (n instanceof Terminal)
            return n.beg;
        NonTerminal nt = (NonTerminal) n;
        int h = headFinder.findHead(s, nt);
        if (h < 1)
            return nt.end;
        else
            return h;
    }

    static final HeadFinder headFinder;

    static {
        Map<String, HeadRules> m = new HashMap<String, HeadRules>();
        String[] clearRules = new String[]{
                "SENTENCE\tl\t^PREP;^SP[CS].*;^CS.*;^GRUP\\.VERB;^S;^SA;^COORD;^CONJ;^GRUP\\.NOM;^SN;^S;.*",
                "S\tl\t^PREP;^SP[CS].*;^COORD;^CONJ;^CS.*;^GRUP\\.VERB;^S;^SA;^COORD;^GRUP\\.NOM;^SN;.*",
                "SA\tl\t^NC.*P.*;^GRUP\\.NOM;\\$;^NC.*S.*;^SADV;^GRUP\\.ADV;^AQA.*;^AQC.*;^V[MAS]P.*;^V[MAS]G.*;^SA;^S\\.A;^GRUP\\.A;^AQS.*;^SN;^GRUP\\.NOM;^D.*;^S;^RG;^RN;.*",
                "S.A\tl\t^NC.*P.*;^GRUP\\.NOM;\\$;^NC.*S.*;^SADV;^GRUP\\.ADV;^AQA.*;^AQC.*;^V[MAS]P.*;^V[MAS]G.*;^S\\.A;^GRUP\\.A;^AQS.*;^SN;^GRUP\\.NOM;^D.*;^S;^RG;^RN;.*",
                "SADV\tr\t^S;^RG;^RN;^SADV;^GRUP\\.ADV;^SP[CS].*;^PREP;^Z.*;^AQA.*;^AQC.*;^S\\.A;^GRUP\\.A;^CONJ;^CS.*;^SN;^GRUP\\.NOM;^AQS.*;^NC.*S.*;.*",
                "SP\tr\t^SP[CS].*;^PREP;^CS.*;^CONJ;^V[MAS]G.*;^V[MAS]P.*;.*",
                "GRUP.A\tl\t^NC.*P.*;^GRUP\\.NOM;\\$;^NC.*S.*;^SADV;^GRUP\\.ADV;^AQA.*;^AQC.*;^V[MAS]P.*;^V[MAS]G.*;^GRUP\\.A;^AQS.*;^SN;^GRUP\\.NOM;^D.*;^S;^RG;^RN;.*",
                "GRUP.ADV\tr\t^RG;^RN;^GRUP\\.ADV;^PREP;^SP.*;^Z.*;^AQA.*;^AQC.*;^GRUP\\.A;^S\\.A;^CS.*;^CONJ;^SN;^GRUP\\.NOM;^AQS.*;^NC.*S.*;.*",
                "GRUP.VERB\tl\t^INFINITIU;^GERUNDI;^PARTICIPI;^PREP;^SP[CS].*;^V[MAS].*[IS].*;^V[MAS]P.*;^V.*C.*;^V[MAS]IP3S.*;^V.*;^V[MAS]G.*;^V[MAS]IP[12]S.*;^GRUP\\.VERB;^SA;^S\\.A;^GRUP\\.A;^NC.*S.*;^NC.*P.*;^GRUP\\.NOM;^SN;^S;.*",
                "INFINITIU\tl\t^VMN.*;^V[MAS]N.*;^V.*;.*",
                "GERUNDI\tl\t^VMG.*;^V[MAS]G.*;^V.*;.*",
                "PARTICIPI\tl\t^VMP.*;^V[MAS]P.*;^V.*;.*",
                "MORFEMA.PRONOMINAL\tl\t^P.*;^SN.*;^GRUP\\.NOM.*;^GRUP\\.VERB;.*",
                "MORFEMA.VERBAL\tl\t^GRUP\\.VERB;^P.*;^SN.*;^GRUP\\.NOM.*;^S;.*",
                "COORD\tr\t^CONJ;^CC.*;^RB;^RN;^SP[CS].*;^PREP;^CS;.*",
                "CONJ\tr\t^CONJ;^CC.*;^RB;^RN;^SP[CS].*;^PREP;^CS;^[^F];.*",
                "INC\tl\t^S;^SN;^GRUP\\.NOM;^GRUP\\.VERB;^SADV;^GRUP.ADV;^SA;^S\\.A;^GRUP\\.A;^PREP;^SP[CS].*;^CONJ;^CS;^D.*;.*",
                "INTERJECCIO\tl\t^I;.*",
                "NEG\tl\t^RN;.*",
                "PREP\tl\t^PREP;^SP[CS].*;^CONJ;^CS$;.*",
                "RELATIU\tl\t^P.*;^SN;^GRUP\\.NOM;^S$;^GRUP\\.VERB;.*",
                "SPEC\tl\t;.*",
                "X\tr\t.*",
                "TOP\tl\t;.*"
        };

        for (String line : clearRules) {
            String[] a = line.split("\\t");
            String lbl = a[0];
            Direction d = (a[1].equals("r") ? Direction.RightToleft : Direction.LeftToRight);
            String[] r = a[2].split(";");
            Rule[] rules = new Rule[r.length];
            int i = 0;
            for (String s : r)
                rules[i++] = new Rule(d, Pattern.compile(s));
            m.put(lbl, new HeadRules(lbl, rules));
        }

        List<String[]> patterns = new ArrayList<String[]>();
        List<Direction> directions = new ArrayList<Direction>();
        directions.add(Direction.RightDis);
        patterns.add(new String[]{"^AQA.*", "^AQC.*", "^GRUP\\.A", "^S\\.A", "^NC.*S.*", "^NP.*", "^NC.*P.*", "^GRUP\\.NOM"});
        directions.add(Direction.LeftToRight);
        patterns.add(new String[]{"^SN", "^GRUP\\.NOM"});
        directions.add(Direction.RightDis);
        patterns.add(new String[]{"\\$", "^GRUP\\.A", "^S\\.A", "^SA"});
        directions.add(Direction.RightToleft);
        patterns.add(new String[]{"^Z.*"});
        directions.add(Direction.RightDis);
        patterns.add(new String[]{"^AQ0.*", "^AQ[AC].*", "^AO.*", "^GRUP\\.A", "^S\\.A", "^RG", "^RN", "^GRUP\\.NOM"});

        List<Rule> rules = new ArrayList<Rule>();
        for (int k = 0; k < patterns.size(); k++) {
            for (String pattern : patterns.get(k))
                rules.add(new Rule(directions.get(k), Pattern.compile(pattern)));
            m.put("SN", new HeadRules("SN", rules));
            m.put("GRUP.NOM", new HeadRules("GRUP.NOM", rules));
        }
        headFinder = new SpanishHeadFinder(m);
    }

    @Override
    public String getDefaultMarkableExtractors() {
        return "NT-SN,NT-GRUP.NOM," +
            "T-SW-Pl," +
            "T-SW-PP," +
            "T-SW-PX," +
            "T-SW-PT," +
            "T-SW-PR," +
            "T-SW-PI," +
            "T-SW-DP," +
            "T-SW-PD," +
            "NER-ALL,NonReferential,SameHeadPruner";
    }

    public void preprocessSentence(Sentence s) {
        if (s.forms[1].equals("Mm"))
            s.tags[1] = "UH";
    }

    static final Set<String> nonReferentials = new HashSet<String>(Arrays.asList("tu", "eso", "nosotros"));

    public Set<String> getNonReferentialTokenSet() {
        return nonReferentials;
    }

    @Override
    public String getDefaultEdgeCreators() {
        return "LeftGraph";
    }


    private static final Pattern COORD_TOKEN_PATTERN =
            Pattern.compile("(e|empero|mas|ni|o|ora|pero|sino|siquiera|u|y|,)", Pattern.CASE_INSENSITIVE);

    public boolean isCoordToken(String string) {
        return COORD_TOKEN_PATTERN.matcher(string).matches();
    }
}


class SpanishHeadFinder extends HeadFinder {

    public SpanishHeadFinder(Map<String, HeadRules> m) {
        super(m);
    }

    public int findHead(CFGNode node) {
        // Skip null node
        if (node == null)
            return -1;
        // skip  alone child
        if (node.beg == node.end)
            return node.beg;

        HeadRules hr = this.m.get(node.getLabel());
        if (hr == null) {
            if (Options.DEBUG)
                System.out.println("Couldn't find head rules for label: " + node.getLabel());
            return -1;

        }
        List<CFGNode> children = node.getChildren();
        int h = findHead(hr.rules, children);
        if (h > 0)
            return h;
        else
            System.out.println("default head for: " + node.getLabel());
            return node.end;

    }


    private int findHead(Rule[] rules, List<CFGNode> children) {
        Iterator<Rule> rule_iter = Arrays.asList(rules).iterator();
        try {
            Rule rule = rule_iter.next();
            while (true) {
                if (rule.d == Direction.LeftToRight) {
                    for (CFGNode n : children)
                        if (rule.headPOSPattern.matcher(n.getLabel()).matches())
                            return findHead(n);

                } else if (rule.d == Direction.RightToleft) {
                    //for (Rule r:rules)
                    for (int k = children.size() - 1; k >= 0; --k) {
                        CFGNode n = children.get(k);
                        if (rule.headPOSPattern.matcher(n.getLabel()).matches())
                            return findHead(n);
                    }
                } else if (rule.d == Direction.LeftDis) {
                    for (CFGNode n : children)
                        if (rule.headPOSPattern.matcher(n.getLabel()).matches())
                            return findHead(n);

                } else if (rule.d == Direction.RightDis) {
                    for (int k = children.size() - 1; k >= 0; --k)
                        for (Rule r : rules) {
                            CFGNode n = children.get(k);
                            if (r.headPOSPattern.matcher(n.getLabel()).matches())
                                return findHead(n);
                        }
                } else {
                    System.err.println("Unknown direction" + rule.d.toString());
                }
                rule = rule_iter.next();

            }
        } catch (NoSuchElementException ex) {
            return 0;
        }
    }
}
