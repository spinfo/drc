package de.uni_koeln.ub.drc.util.tocparser

/**
 * This is an abstract class that serves as data model
 * for each section. It provides a toXML method which 
 * generates corresponding XML.
 * @author Sebastian Rose
 */
abstract class Section {
	
	override def toString = name
	
	type Data = List[Map[String, List[(_, _)]]]
	
	val name: String
	val subsections: List[Map[String, Data]]

	def toXML =
		
<section name={ name }>

{
for (subsection <- subsections) yield
<subsection name={ subsection.keySet.first }>

	{
	for (rangeList <- subsection(subsection.keySet.first);
	volNum <- rangeList.keySet) yield
<volume>
<number>{ volNum }</number>

		{
		for (data <- rangeList(volNum)) yield
<data>
<start>{ data._1 }</start>
<end>{ data._2 }</end>
</data>
		}
		
</volume>
	}
	
</subsection>
}

</section>
	
}