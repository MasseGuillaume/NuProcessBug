object Main {
  def main(args: Array[String]): Unit = {
    println("started")
    while(true) {
      println("Dont kill me!")
      Thread.sleep(500)
    }
  }
}
