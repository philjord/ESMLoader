package esfilemanager.loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import esfilemanager.Point;
import esfilemanager.common.PluginException;
import esfilemanager.common.data.plugin.PluginGroup;
import esfilemanager.common.data.plugin.PluginRecord;
import esfilemanager.common.data.record.Subrecord;
import tools.io.ESMByteConvert;
import tools.io.FileChannelRAF;

public class WRLDExtSubblock extends PluginGroup {
	public long							fileOffset;
	public int							length;
	public int							x;
	public int							y;

	private Map<Point, FormToFilePointer>	CELLByXY	= null;

	public WRLDExtSubblock(byte[] prefix, long fileOffset, int length) {
		super(prefix);
		this.fileOffset = fileOffset;
		this.length = length;

		int intValue = ESMByteConvert.extractInt(groupLabel, 0);
		x = intValue >>> 16;
		if ((x & 0x8000) != 0)
			x |= 0xffff0000;
		y = intValue & 0xffff;
		if ((y & 0x8000) != 0)
			y |= 0xffff0000;
	}

	public FormToFilePointer getWRLDExtBlockCELLByXY(Point point, FileChannelRAF in)
			throws IOException, DataFormatException, PluginException {
		// must hold everyone up so a single thread does the load if needed
		synchronized (this) {
			if (CELLByXY == null)
				loadAndIndex(in);
		}

		return CELLByXY.get(point);
	}

	private void loadAndIndex(FileChannelRAF in) throws IOException, DataFormatException, PluginException {
		CELLByXY = new HashMap<Point, FormToFilePointer>();
		synchronized (in) {
			in.seek(fileOffset);
			int dataLength = length;
			byte prefix[] = new byte[headerByteCount];

			FormToFilePointer formToFilePointer = null;

			while (dataLength >= headerByteCount) {
				long filePositionPointer = in.getFilePointer();

				int count = in.read(prefix);
				if (count != headerByteCount)
					throw new PluginException("Record prefix is incomplete");
				dataLength -= headerByteCount;
				String type = new String(prefix, 0, 4);
				int length = ESMByteConvert.extractInt(prefix, 4);

				if (type.equals("GRUP")) {
					length -= headerByteCount;
					int gt = prefix [12] & 0xff;

					if (gt == PluginGroup.CELL) {
						formToFilePointer.cellChildrenFilePointer = filePositionPointer;
						// now skip the group
						in.skipBytes(length);
					} else {
						System.out.println("Group Type " + gt + " not allowed as child of WRLD ext sub block group");
					}
				} else if (type.equals("CELL")) {
					int formID = ESMByteConvert.extractInt3(prefix, 12);

					formToFilePointer = new FormToFilePointer(formID, filePositionPointer);

					PluginRecord rec = new PluginRecord(prefix);
					rec.load("", in, length);
					// find the x and y
					List<Subrecord> subrecords = rec.getSubrecords();
					int x = 0;
					int y = 0;
					for (int i = 0; i < subrecords.size(); i++) {
						Subrecord sr = subrecords.get(i);
						byte[] bs = sr.getSubrecordData();

						if (sr.getSubrecordType().equals("XCLC")) {
							x = ESMByteConvert.extractInt(bs, 0);
							y = ESMByteConvert.extractInt(bs, 4);
						}
					}
					//we do index now
					Point p = new Point(x, y);
					CELLByXY.put(p, formToFilePointer);
				} else {
					System.out.println("What the hell is a type " + type + " doing in the WRLD ext sub block group?");
				}

				//prep for next iter
				dataLength -= length;
			}

			if (dataLength != 0) {
				if (getGroupType() == 0)
					throw new PluginException("Group " + getGroupRecordType() + " is incomplete");
				else
					throw new PluginException("Subgroup type " + getGroupType() + " is incomplete");
			}
		}

	}
}
