package com.ma2.app;


import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.flickrjandroid.Flickr;
import com.googlecode.flickrjandroid.FlickrException;
import com.googlecode.flickrjandroid.people.User;
import com.googlecode.flickrjandroid.photos.PhotoList;

public class MainActivity extends Activity {

	private final int number_pics = 10;
	private ImagePagerAdapter adapter;
	private List<Bitmap> list_pictures = new ArrayList<Bitmap>();
	private Bitmap[] bitmaps = new Bitmap[number_pics];

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//		builder.show();
		new getPictures().execute(0);
		ViewPager viewPager = (ViewPager) findViewById(R.id.picture);
		adapter = new ImagePagerAdapter();
		viewPager.setAdapter(adapter);
	}

	public OnClickListener onClick(Builder builder, EditText tv) {
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(0,0,0,"Send this picture");
		menu.add(0, 1, 1, "Listen for Surface");
		menu.add(0, 2, 2, "Receive picture");
		return true;
	}

	@Override 
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId() == 0){ // send picture
			ViewPager viewPager = (ViewPager) findViewById(R.id.picture);
			int i = viewPager.getCurrentItem();			
			//			new SendPicture().execute(i);
		}else if(item.getItemId() == 1){ // listen for surface
			new SendPictures().execute(0);			
		}else if(item.getItemId() == 2){
			 // listen for surface
			new receivePicture().execute(0);
		}
		
		//		mImages[i];
		return super.onOptionsItemSelected(item);
	}

	public void showDialog(String title, String message) {

		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//add OK button
		builder.setPositiveButton(R.string.OK, null);
		// 2. Chain together various setter methods to set the dialog characteristics
		builder.setMessage(message).setTitle(title);
		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
		dialog.show();

	}

	private void addPhoto(InputStream input, int i){
		//		bitmaps[i] = BitmapFactory.decodeStream(input);
		list_pictures.add(BitmapFactory.decodeStream(input));
	}

	private void savePicture(byte[] buffer){

		InputStream in = new ByteArrayInputStream(buffer);
		list_pictures.add(BitmapFactory.decodeStream(in));
	}

	private class SendPictures extends AsyncTask<Integer, Integer, Boolean>{
		ProgressDialog progress;
		Socket socket;
		ServerSocket serverSocket;
		DataOutputStream out;

		@Override
		protected Boolean doInBackground(Integer... picture) {
			// TODO Auto-generated method stub

			try{
				serverSocket = new ServerSocket(8000);
				socket = serverSocket.accept();
				System.out.println("socket accepted - SendPictures()");
//				showDialog("Sending pics","Connection detected... sending pics");
				out = new DataOutputStream(socket.getOutputStream());
				out.writeInt(number_pics);
				for(int i=0; i<number_pics; i++){
					publishProgress(0);
					Bitmap bitmap =  list_pictures.get(i);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					if(!bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)){
						return false;
					}
					byte [] byte_arr = stream.toByteArray();
					int sent = 0;
					int size_pic = byte_arr.length;
					System.out.println("PIC"+i+" size "+size_pic);
					out.writeInt(size_pic);
					while(sent < byte_arr.length){
						if((byte_arr.length - sent) > 1024){
							out.write(byte_arr, sent, 1024);
							sent = sent + 1024;
						}else{
							out.write(byte_arr, sent, byte_arr.length - sent);
							sent = byte_arr.length;
						}
						System.out.println(sent + "-sent PIC"+i);
						publishProgress((sent*100)/byte_arr.length);
					}
					//				
					publishProgress(100);
					Thread.sleep(1000);
				}//end-for
				System.out.println("pics sent- SendPictures()");
				out.close();
				socket.close();
				serverSocket.close();
			}
			catch(Exception e){
				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			setProgress(progress[0]);
		}

		@Override
		protected void onPreExecute(){
			//			progress = new ProgressDialog(MainActivity.this);
			//			progress.setCancelable(true);
			//			progress.setMessage("Waiting for Surface!");
			//			progress.setMessage("b");
			//			progress.show();
			System.out.println("about to send pics - SendPictures()");
		}

		@Override
		protected void onPostExecute(Boolean result){
			String title,message;
			if(result){
				title = "Yeeaaahy!";
				message = "The pictures were sent!";
			}else{
				title = "Buuuu!!!";
				message = "Error when sending pictures";
			}
			showDialog(title,message);
			new receivePicture().execute(0);
		}

	}

	private class getPictures extends AsyncTask<Integer, Integer, Boolean>{
		ProgressDialog progress;
		Flickr f = new Flickr("32b2e2482b4780568b7f5ec33526cd01", "3d8428f75da1e7e9");
		User user;
		PhotoList photos;

		@Override
		protected Boolean doInBackground(Integer... picture) {
			// TODO Auto-generated method stub
			//flickr
			try {
				System.out.println("about to import pics - getPictures()");
				user = f.getPeopleInterface().findByUsername("daniel.lujan.v");
				photos = f.getPeopleInterface().getPublicPhotos(user.getId(), number_pics, 1);
				for(int i = 0; i< number_pics;i++){
					addPhoto(f.getPhotosInterface().getImageAsStream(photos.get(i), 1) ,i);
				}				
				System.out.println("pics imported- getPictures()");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (FlickrException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onPreExecute(){
			System.out.println("about to import pics - getPictures()0");
			//			progress = ProgressDialog.show(MainActivity.this, "Sending picture", null, true);
		}

		@Override
		protected void onPostExecute(Boolean result){
			adapter.notifyDataSetChanged();
			new SendPictures().execute(-1);
		}
	}

	private class receivePicture extends AsyncTask<Integer,Integer,Boolean>{
		ServerSocket serverSocket;
		Socket socket;
		DataInputStream in;
		@Override
		protected Boolean doInBackground(Integer... params) {
			// TODO Auto-generated method stub
			try {
				serverSocket = new ServerSocket(8001);
				System.out.println("Waiting connection");
				socket = serverSocket.accept();
				System.out.println("Connection accepted - waiting pic");
				in = new DataInputStream(socket.getInputStream());
				byte[] buffer; 
				byte[] pic_size = new byte[4];
				in.read(pic_size,0,4);
				int size = ByteBuffer.wrap(pic_size).order(ByteOrder.LITTLE_ENDIAN).getInt();
				System.out.println("pic size "+ size);
				int read = 0;
				buffer = new byte[size];
				while(read < size ){
					if((size - read) >= 1024){
						read += in.read(buffer, read, 1024);
					}else{
						read += in.read(buffer, read, buffer.length - read);
					}
					System.out.println("read - "+read);
				}
				in.close();
				socket.close();
				serverSocket.close();
				System.out.println("saving pic");
				savePicture(buffer);
				System.out.println("pic saved");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result){
			String title,message;
			if(result){
				title = "Yeeaaahy!";
				message = "The picture was received!";
			}else{
				title = "Buuuu!!!";
				message = "Error when receiving picture";
			}
			adapter.notifyDataSetChanged();
			showDialog(title,message);
		}

	}

	private class ImagePagerAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return list_pictures.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == ((ImageView) object);
		}

		@Override
		public void startUpdate(ViewGroup container){
			int i= ((ViewPager) container).getCurrentItem();
			TextView tv = (TextView) findViewById(R.id.picName);
			tv.setText((i+1)+" of "+ list_pictures.size());
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Context context = MainActivity.this;
			ImageView imageView = new ImageView(context);
			int padding = 10;
			//			int padding = 5;
			imageView.setPadding(padding, 0,0, 0);
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			//			imageView.setImageBitmap(decodeSampledBitmapFromResource(getResources(), mImages[position], 2048, 2048));
			imageView.setImageBitmap(list_pictures.get(position));
			((ViewPager) container).addView(imageView, 0);
			return imageView;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((ImageView) object);
		}

	}

}
