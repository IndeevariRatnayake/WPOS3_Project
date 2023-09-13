package com.harshana.wposandroiposapp.Utilities.XMlHelper;

public class ChildTag extends Tag
{
	public ChildTag(String _tagName,String _value)
	{
		tagName = _tagName;
		value  = _value;
	}
	public String getXMLString()
	{
		//String xml = "<" + tagName + ">" + value + "</" + tagName + ">\n";
		String xml = "<" + tagName + ">" + value + "</" + tagName + ">";
		return xml;
	}
}
