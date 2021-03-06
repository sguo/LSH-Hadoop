package semvec.mahout;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class SVDriver {

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args)
    .getRemainingArgs();
    if (otherArgs.length != 2) {
      System.err.println("Usage: SVDriver <in> <out>");
      System.exit(2);
    }
    Job job = new Job(conf, "word count");
    job.setJarByClass(SVDriver.class);
    job.setMapperClass(UserItemPrefMapper.class);
    job.setReducerClass(UserItemPrefReducer.class);
    job.setOutputKeyClass(LongWritable.class);
    job.setOutputValueClass(TupleWritable.class);
    FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
    rmdir(new File(otherArgs[1]));
    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }

  static void rmdir(File dir) {
    if (null == dir.listFiles())
      return;
    for (File f : dir.listFiles()) {
      f.delete();
    }
    dir.delete();
  }
}
