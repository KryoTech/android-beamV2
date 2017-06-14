package com.example.lenovo.test;

import java.util.Random;

class Config {
  final static String HOST = "beamholychild.xyz/crud";

  static int gen() {
    Random r = new Random( System.currentTimeMillis() );
    return ((1 + r.nextInt(2)) * 100 + r.nextInt(100));
  }
}