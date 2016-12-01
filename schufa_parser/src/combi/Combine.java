package combi;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Combine {

    public static ArrayList <String> combineTwo(List <String> x, List <String> y  ){
        ArrayList <String> v = new ArrayList<String>();
        for (int i = 0; i < x.size() ; i++ )
            for (int j = 0; j < y.size() ; j++) {
                v.add( x.get(i) + y.get(j) );
            }
        return v;
    }


    public static ArrayList <String> combineBatch (List <List <String>> coll){
        int total = coll.size();
        ArrayList <String> result = new ArrayList <String> ();
        for (int i = 0; total-1 > i ; i ++ ){
            if (i == 0){
                result = combineTwo(coll.get(i), coll.get(i+ 1) );
            }else {
                result = combineTwo(result, coll.get(i + 1) );
            }
        }
        return result;
    }


    public static void main (String... argc){
        String[] x = { "x" };
        String[] y = { "a", "b", "c" };
        String[] z = { "1", "2", "3", "4" };


        ArrayList input = new ArrayList();
        input.add(Arrays.asList(x));
        input.add(Arrays.asList(y));
        input.add(Arrays.asList(z));

        ArrayList <String> output = combineBatch( input);

        System.out.println(output);


    }

}
