package Ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;

public class Ontology extends BeanOntology {
	private static Ontology theInstance = new Ontology("my_ontology");
	
	public static Ontology getInstance() {
		return theInstance;
	}
	
	private Ontology(String name) {
		super(name);
		try {
			add("elements");
		}
		catch(BeanOntologyException e) {
			e.printStackTrace();
		}
	}

}
