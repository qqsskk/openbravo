package org.wirabumi.bprl.ad_process;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class Utility {
	
	public static BufferedImage GetQRCodeImage(String qrcode){
		byte[] b = qrcode.getBytes();
		//convert the byte array into a UTF-8 string		
		String data;
		try {
		    data = new String(b, "UTF8");
		}
		catch (UnsupportedEncodingException e) {
		 //the program shouldn't be able to get here
		 return null;
		}

		//get a byte matrix for the data
		ByteMatrix matrix=null;;
		com.google.zxing.Writer writer = new QRCodeWriter();
		try {
			matrix = writer.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 96, 96);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		

		//generate an image from the byte matrix
		int width = matrix.getWidth(); 
		int height = matrix.getHeight(); 

		byte[][] array = matrix.getArray();

		//create buffered image to draw to
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		//iterate through the matrix and draw the pixels to the image
		for (int y = 0; y < height; y++) { 
		 for (int x = 0; x < width; x++) { 
		  int grayValue = array[y][x] & 0xff; 
		  image.setRGB(x, y, (grayValue == 0 ? 0 : 0xFFFFFF));
		 }
		}
		
		return image;
	}
	
	public static String GetCostcenterIdentifier(String costcenterID){
		Costcenter costcenter = OBDal.getInstance().get(Costcenter.class, costcenterID);
		if (costcenter==null)
			return "invalid cost center id";
		StringBuilder sb = new StringBuilder();
		sb.append(costcenter.getSearchKey()).append(" - ").append(costcenter.getName());
		return sb.toString();
	}
	
	public static String GetProductGroupInOutLine(String inoutID){
		ShipmentInOut goodsShipment = OBDal.getInstance().get(ShipmentInOut.class, inoutID);
		if (goodsShipment==null)
			return "invalid goods shiment id";
		List<String> productGroupList = new ArrayList<String>();
		for (ShipmentInOutLine inoutLine : goodsShipment.getMaterialMgmtShipmentInOutLineList()){
			String productGroup = inoutLine.getProduct().getProductCategory().getName();
			if (!productGroupList.contains(productGroup))
				productGroupList.add(productGroup);
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<productGroupList.size(); i++){
			if (i==0)
				sb.append(productGroupList.get(i));
			else
				sb.append(", ").append(productGroupList.get(i));
		}
		
		return sb.toString();
	}

}
