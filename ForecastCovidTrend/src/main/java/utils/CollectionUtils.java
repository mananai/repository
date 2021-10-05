package utils;

import java.util.Collection;
import java.util.stream.Collectors;

public class CollectionUtils {

	public static String toString(String elementFormat, double[] array) {
		String result="[";
		for(int i = 0 ; i <array.length ; i++) {
			if ( i > 0)
				result += ", ";
			result += String.format(elementFormat, array[i]);
		}
		result += "]";
		return result;
	}
	
	public static String toString(String elementFormat, Collection<?> collection) {
		return "["+ collection.stream().map(e->String.format(elementFormat, e)).collect(Collectors.joining(", ")) + "]";
	}
}
