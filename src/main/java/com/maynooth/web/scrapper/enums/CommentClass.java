package com.maynooth.web.scrapper.enums;

public enum CommentClass {

	FULL_CONTAIN(1,"comment full contain"),
	ONELINE(2, "comment oneline"),
	HIDDEN(3, "comment hidden");
	
	private int id;
	private String desc;
	
	CommentClass(int id, String desc)
	{
		this.id = id;
		this.desc = desc;
	}

	public int getId() {
		return id;
	}

	public String getDesc() {
		return desc;
	}
	
	public static CommentClass getByDesc(String desc)
	{
		for(CommentClass cls : CommentClass.values())
			if(cls.getDesc().equalsIgnoreCase(desc))
				return cls;
		
		return null;
	}
}
