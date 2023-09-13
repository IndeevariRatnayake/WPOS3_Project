package com.harshana.wposandroiposapp.Utilities.XMlHelper;

import java.util.LinkedList;
import java.util.Queue;

public class XMLHelper {	
	
	private Queue<ParentTag> parentTags = null;
	
	private XMLHelper()
	{
		parentTags = new LinkedList<>();
	}
	private static XMLHelper instance = null;
	public static XMLHelper getInstance()
	{
		if (instance == null)
			instance = new XMLHelper();
		instance.clearTags();
		return instance;
	}


	public void clearTags()
	{
		parentTags.clear();;
	}
	public void addParentTag(ParentTag tag)
	{
		parentTags.add(tag);
	}
	
	
	public String generateXML() throws Exception
	{
		if (parentTags.size() == 0)
			throw new Exception("No parent tags to generate XML");
		
		String tagString  = "";
		
		for (ParentTag tag : parentTags)
			tagString += (  tag.getXMLString());
		//tagString += (  tag.getXMLString() + "\n");
		
		
		return tagString;
	}
	
	
	public String GetValue(String tagName,String xmlString)
	{
		if (tagName == null || xmlString == null) return null;
		
		int tagIndex = 0 ;
		if ((tagIndex = xmlString.indexOf(tagName))  < 0)
			return null;
		
		//found the tag and should attempt to extract the value
		
		tagIndex++;
		int end = 0 ; 
		if ((end = xmlString.indexOf("</",tagIndex)) < 0)
			return null;
		
		//extract the value
		tagIndex += tagName.length();
		
		String value = xmlString.substring(tagIndex,end);
		
		return value;
	}
	
}
