package ServiceTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class DHExp3 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		filterMutation(path1 + "Applications/LoanApproval/Mutations/", "mutations.txt",LASF1);
		filterMutation(path1 + "Applications/LoanApproval/Mutations/", "mutations.txt",LASF2);		
		filterMutation(path1 + "Applications/LoanApproval/INCMutations/", "mutations.txt",LAIF1);
		
		filterMutation(path1 + "Applications/BookOrdering/Mutations/", "mutations.txt",BOSF1);
		filterMutation(path1 + "Applications/BookOrdering/Mutations/", "mutations.txt",BOSF2);		
		filterMutation(path1 + "Applications/BookOrdering/INCMutations/", "mutations.txt",BOIF1);

		filterMutation(path1 + "Applications/SupplyChain/Mutations/", "mutations.txt",SCSF1);
		filterMutation(path1 + "Applications/SupplyChain/Mutations/", "mutations.txt",SCSF2);		
		filterMutation(path1 + "Applications/SupplyChain/INCMutations/", "mutations.txt",SCIF1);

		filterMutation(path1 + "Applications/Insurance/Mutations/", "mutations.txt",INSF1);
		filterMutation(path1 + "Applications/Insurance/Mutations/", "mutations.txt",INSF2);		
		filterMutation(path1 + "Applications/Insurance/INCMutations/", "mutations.txt",INIF1);

		filterMutation(path1 + "Applications/Auction/Mutations/", "mutations.txt",ACSF1);
		filterMutation(path1 + "Applications/Auction/Mutations/", "mutations.txt",ACSF2);		
		filterMutation(path1 + "Applications/Auction/INCMutations/", "mutations.txt",ACIF1);

	}

	private static String path1="tmp/";
	
	private static int[] LASF1={18, 24, 25, 28, 32, 40, 41, 42, 45, 49, 50, 60, 63, 66};
	private static int[] LAIF1={0, 2, 4, 6, 9, 10};
	private static int[] BOSF1={6, 8, 9, 11, 12, 15, 20, 23, 26, 31, 43, 49, 52, 64, 68, 71, 83, 86, 90, 91, 92, 93, 94, 95, 96, 97, 98, 100, 106, 114, 127, 133, 144, 160, 170, 173, 182, 191, 192, 193, 195, 196, 32, 33, 34};
	private static int[] BOIF1={7, 8, 9, 10, 11, 12, 13, 16, 17, 20, 22, 26, 38, 41, 44, 45, 48};
	private static int[] SCSF1={37, 99, 101, 114, 116, 117, 120, 137, 139, 140, 143, 144, 168, 170, 171, 175, 177, 179, 186, 187, 188, 189, 191, 200, 201, 210, 211, 212, 213, 215, 224, 225, 228, 229, 230, 231, 233, 242, 243, 253, 254, 271, 272, 287}; 
	private static int[] SCIF1={0, 3, 5, 7, 10, 12, 14, 17, 19, 21, 24, 26, 28, 31, 33};
	private static int[] ACSF1={0, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 26, 47, 49, 50, 53, 56, 65, 70, 71, 72, 73, 74, 75, 76, 77, 90, 96, 115, 116, 117, 119, 120, 121, 138, 139, 153, 154, 155, 158, 159, 160, 2, 36, 42, 43, 48, 68, 110, 111, 112, 113, 114, 118, 122, 123, 124, 125, 128, 129, 130, 131, 132, 163, 164, 165, 166}; 
	private static int[] ACIF1={1, 2, 3, 4, 16, 17, 18, 19, 26, 27, 28, 29, 31, 32, 33, 34, 46, 47, 48, 49, 56, 57, 58, 59, 61, 62, 63, 64, 66, 67, 68, 69, 71, 72, 73, 74, 76, 77, 78, 79, 81, 82, 83, 84, 86, 87, 88, 89, 91, 92, 93, 94, 96, 97, 98, 99, 101, 102, 103, 104, 106, 107, 108, 109, 111, 112, 113, 114, 116, 117, 118, 119, 121, 122, 123, 124, 136, 137, 138, 139, 146, 147, 148, 149};
	private static int[] INSF1={31, 35, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 137, 138, 163, 165, 169, 171, 172, 177, 185, 187, 218, 220, 26, 27, 28, 159, 160, 161, 180, 184, 186, 188, 197, 198, 209, 228, 229, 230, 249, 250, 251, 252, 253, 254, 255, 265, 266, 267}; 
	private static int[] INIF1={42, 52, 55, 62, 65, 72, 74, 92, 102, 112, 116, 122, 132};

	
	private static int[] LASF2={12, 13, 18, 25, 26, 33, 34, 35, 36};
	private static int[] LAIF2={};
	private static int[] BOSF2={44, 45, 46};
	private static int[] BOIF2={};
	private static int[] SCSF2={218, 219, 220}; 
	private static int[] SCIF2={};
	private static int[] ACSF2={1, 2, 3, 21, 24, 25, 26, 27, 28, 29}; 
	private static int[] ACIF2={};
	private static int[] INSF2={26, 30, 31, 33, 34, 35, 36, 37, 46, 103, 133, 135, 136, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 152, 153, 154, 155, 156, 165, 167, 168, 207, 208, 213, 218, 219, 220}; 
	private static int[] INIF2={};

	
	private static void filterMutation(String path, String name, int[] filter) {
		
		ArrayList<String[]> result = new ArrayList<String[]>();
		ArrayList<String[]> newml = new ArrayList<String[]>();
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(path + name));
			String line = reader.readLine();
		
			while(line!=null) {
				if(!line.isEmpty()) {
					String[] conts = line.split("\\s+");
					result.add(conts);
				}
				
				line = reader.readLine();
			}
			reader.close();
			
			for(int index:filter) {
				newml.add(result.get(index));
			}
			
			//remove the file
			for(String[] fn: newml) {
				for(String item:fn) {
					String mtn = path1 + item;
					File f = new File(mtn);
					f.delete();
				}
			}
			
			//update the mutation list
			result.removeAll(newml);
			BufferedWriter writer = new BufferedWriter(new FileWriter(path + name));
			for(String[] fn: result) {
				line="";
				for(String item: fn) 
					line += item + " ";
				
				writer.write(line);
				writer.newLine();
			}
			
			writer.close();
		
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		
	}
	
}
