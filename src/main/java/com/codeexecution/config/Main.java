//package com.codeexecution.config;
//
//import java.util.*;
//public class Main {
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//
//        // Read input count
//        int n = scanner.nextInt();
//        scanner.nextLine();  // Consume newline
//
//        // Read main input
//        List<Integer> arr = new ArrayList<>();
//        for (int i = 0; i < n; i++) {
//            arr.add(scanner.nextInt());
//        }
//
//        // Execute solution
//        int result = Solution.migratoryBirds(arr);
//        System.out.println(result);
//    }
//}
//
//class Solution {
//    public static int migratoryBirds(List<Integer> arr) {
//        Map<Integer, Integer> map = new HashMap<>();
//        for (int n : arr) {
//            map.put(n, map.getOrDefault(n, 0) + 1);
//        }
//        int max = 0, best = Integer.MAX_VALUE;
//        for (int k : map.keySet()) {
//            int freq = map.get(k);
//            if (freq > max || (freq == max && k < best)) {
//                max = freq;
//                best = k;
//            }
//        }
//        return best;
//    }
//}