void main() {
  int n;
  n = 2;
  if (n >= 1) {
    if (n == 1) {
      print_s((char*)"2a");
    } else if (n > 1) {
      print_s((char*)"2b");
    } else {
      print_s((char*)"2c");
    }
  } else {
    print_s((char*)"1b");
  }
}


