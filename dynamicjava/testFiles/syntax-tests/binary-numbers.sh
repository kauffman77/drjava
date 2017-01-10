#!/bin/bash


java -jar dynamicjava.jar -b <<EOF
// accept 0b for binary constants
int binary0 = 0b0101110;
binary0
int binary1 = 0b1011101;
binary1
// accept 0B as well
int b = 0B0110110000;
b
// Fix capital X in 0X bug
int hex = 0X1af
hex

// Bugs for leading 0 in hex
int x = 0x10
x
int y = 0x0010
y
y==16
int hex = 0x01af
hex
EOF
