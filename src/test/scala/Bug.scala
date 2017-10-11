import org.scalatest.FunSuite

import com.zaxxer.nuprocess.{NuAbstractProcessHandler, NuProcess, NuProcessBuilder}

import scala.collection.JavaConverters._

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.io.File

import java.util.concurrent.CountDownLatch

class Bug extends FunSuite {
  test("bug") {
    val startSignal = new CountDownLatch(1)
    val startedSignal = new CountDownLatch(1)
    val exitSignal = new CountDownLatch(1)

    // val command = List(getClass.getResource("/loop.sh").getFile)
    // new File(command.head).setExecutable(true)

    val command = List("sbt", "run")

    val workingDir = Paths.get("./scastie")

    val pb = new NuProcessBuilder(command.asJava)
    pb.setProcessListener(new NuAbstractProcessHandler {
      override def onPreStart(nuProcess: NuProcess): Unit = {
        nuProcess.setProcessHandler(new NuAbstractProcessHandler {
          private def show(buffer: ByteBuffer, tpe: String, closed: Boolean): Unit = {
            val out = StandardCharsets.UTF_8.decode(buffer).toString();

            val started = 
              if (out.contains("started")) {
                startedSignal.countDown()
                true
              } else {
                false
              }

            println(s"$tpe (closed = $closed, started = $started): $out")
          }

          override def onStart(nuProcess: NuProcess): Unit = {
            startSignal.countDown()
            println(s"start: ${nuProcess.getPID}")
          }

          override def onStderr(buffer: ByteBuffer, closed: Boolean): Unit = {
            show(buffer, "err", closed)
          }

          override def onExit(exitCode: Int): Unit = {
            exitSignal.countDown()
            println(s"exit: $exitCode")
          }

          override def onStdout(buffer: ByteBuffer, closed: Boolean): Unit = {
            show(buffer, "out", closed)
          }
        })
      }
    })
    pb.setCwd(workingDir)

    val process = pb.start()

    startSignal.await()
    println("--- Start ---")
    assert(process.isRunning)

    startedSignal.await()
    println("--- Run Start ---")
    
    Thread.sleep(1000)

    println("--- Killing ---")
    process.destroy(true)

    exitSignal.await()
    println("--- Exited ---")
    assert(!process.isRunning)
  }
}
