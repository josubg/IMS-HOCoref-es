package ims.hotcoref.markables;

import ims.hotcoref.data.Sentence;
import ims.hotcoref.data.Span;

import java.util.Set;

public class TerminalExtractorSW extends TerminalExtractor{

	public TerminalExtractorSW(String tag){
		super(tag);
	}

	@Override
	public void extractMarkables(Sentence s, Set<Span> sink, String docName) {
		for(int i=1;i<s.tags.length;++i){
			if(s.tags[i].startsWith(tag))
				sink.add(s.getSpan(i, i));
		}
	}

	public String toString(){
		return "T-SW-"+tag;
	}


}
