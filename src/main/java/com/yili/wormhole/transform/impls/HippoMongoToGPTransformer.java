package com.yili.wormhole.transform.impls;

import com.dp.nebula.common.utils.TypeConvertionHelper;
import com.yili.wormhole.common.AbstractTransformer;
import com.yili.wormhole.common.DefaultLine;
import com.yili.wormhole.common.interfaces.ILine;
import com.yili.wormhole.transform.utils.FieldTransformUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HippoMongoToGPTransformer extends AbstractTransformer{
	
	private final Log s_logger = LogFactory.getLog(HippoMongoToGPTransformer.class);
	
	@Override
	public ILine transform(ILine line) {
		ILine result = new DefaultLine();
		String statDate = null;
		for(int i = 0; i < line.getFieldNum(); i++) {
			if(i==0 || i==2 || i==4) {
				long unixTime = TypeConvertionHelper.convertStringToInteger(line.getField(i), 0);
				if(unixTime == 0) {
					s_logger.error("Unix time error for:" + line.getField(i));
				}
				//String dateStr = "2012-07-19 00:00:00";
				String dateStr = FieldTransformUtils.fromUnixTime(unixTime);
				result.addField(dateStr);
				if(i==0) {
					statDate = dateStr.substring(0,dateStr.indexOf(' '));
				}
			} else {
				result.addField(line.getField(i));
			}
		}
		result.addField(statDate);
		return result;
	}
	
}
