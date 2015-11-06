package TES4Gecko;

public class PluginTopic
{
	private int formID;

	private String editorID;

	private boolean deleted;

	public PluginTopic(int formID)
	{
		this(formID, new String());
	}

	public PluginTopic(int formID, String editorID)
	{
		this.formID = formID;
		this.editorID = editorID;
		this.deleted = false;
	}

	public int getFormID()
	{
		return this.formID;
	}

	public String getEditorID()
	{
		return this.editorID;
	}

	public boolean isDeleted()
	{
		return this.deleted;
	}

	public void setDelete(boolean deleted)
	{
		this.deleted = deleted;
	}
}

/* Location:           C:\temp\TES4Gecko\
 * Qualified Name:     TES4Gecko.PluginTopic
 * JD-Core Version:    0.6.0
 */