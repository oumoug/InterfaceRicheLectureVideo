package fr.enssat.gaye.interfacerichelecturevideo;

import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity {
    private VideoView videoRoute66;
    private MediaController controlleurLectureVideo;
    private WebView pageSynopsis;
    private ProgressDialog mDialog;
    private HorizontalScrollView scrollBar;
    private LinearLayout layoutChapitre;
    private MapView mMapView;
    private int ancienPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(getString(R.string.MAPVIEW_BUNDLE_KEY));
        }
        mMapView = findViewById(R.id.mapview);
        mMapView.onCreate(mapViewBundle);
        AfficheVideo();
        chargeWebView();
        chargechapitrage();
        chargeMap();
    }
    /*****************************************
     * enregistre l'etat de la carte           *
     * et la position de la lecture            *
    ***************************************/
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(getString(R.string.MAPVIEW_BUNDLE_KEY));
        if (mapViewBundle != null) {
            mapViewBundle = new Bundle();
            outState.putBundle(getString(R.string.MAPVIEW_BUNDLE_KEY), mapViewBundle);
        }
        outState.putInt("duree", ancienPosition);
        Log.i("savePosition", Integer.toString(ancienPosition));
        mMapView.onSaveInstanceState(mapViewBundle);

    }
    /*************************************************
     * télécharge la vidéo l'affiche dans la videoView *
     * Affiche une barre de progression en attendant le téléchargement de la vidéo
    *****************************************************/
    public void AfficheVideo(){
        videoRoute66=(VideoView) findViewById(R.id.videoViewRoute66);
        videoRoute66.setVideoURI(Uri.parse(getString(R.string.url_videoRoute66)));
        controlleurLectureVideo=new MediaController(MainActivity.this);
        mDialog = new ProgressDialog(MainActivity.this);
        mDialog.setMessage("Chargement en cours");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();
        videoRoute66.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            public void onPrepared(MediaPlayer mp) {
                videoRoute66.setMediaController(controlleurLectureVideo);
                controlleurLectureVideo.setAnchorView(videoRoute66);
                videoRoute66.requestFocus();
                videoRoute66.start();
                mDialog.dismiss();
            }
        });
    }
    /****************************************************
     * télécharge une page web et l'affiche dans une webView

     *****************************************************/
    public void chargeWebView(){
        pageSynopsis= (WebView) findViewById(R.id.webView1);
        pageSynopsis.setWebViewClient(new WebViewClient());
        pageSynopsis.setWebChromeClient(new WebChromeClient());
        pageSynopsis.getSettings().setJavaScriptEnabled(true);
        pageSynopsis.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                pageSynopsis.loadUrl(url);
                return false;
            }
        });
        pageSynopsis.loadUrl(getString(R.string.url_videoSynopsis));
    }
    /************************************************************
     * défini l'action a faire lorsqu'on clique sur un bouton
     * dans ce cas ci met la vidéo à une position defini par le tag du bouton
    * ************************************************************************/
    private View.OnClickListener listenerScrollBar=new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            videoRoute66.seekTo((int)view.getTag()*1000);
        }
    };
    /****************************************************************
     * défini l'action à faire lorqu'on clique sur un waypoint de la carte google
     * dans ce vas met la video à la position defini par le tag du waypoint
    * **************************************************************************/
    private GoogleMap.OnMarkerClickListener ListenerWayPointGoogleMap= new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            int timestamp = (int) marker.getTag();
            videoRoute66.seekTo(timestamp * 1000);
            return false;
        }
    };
    /****************************************************************************
     * charge le fichier json contenant les chapitres
     * créee des boutons pour le chapitrage
     * ajoute un listener au bouton
     * ajoute les bouton aux layout
     *****************************************************************************/
    public void chargechapitrage(){
       JSONObject objetJson;
        JSONArray tableauJson;
        int position;
        String titre;
        Button bouton;
        layoutChapitre=(LinearLayout) findViewById(R.id.linearLayout1);
        InputStream inputStream=getResources().openRawResource(R.raw.chapitre);
        ByteArrayOutputStream byteArrayOutputStream= new  ByteArrayOutputStream();
        try {
            int elem = inputStream.read();
            while (elem != -1) {
                byteArrayOutputStream.write(elem);
                elem = inputStream.read();
            }
            inputStream.close();
            try {
                objetJson = new JSONObject(byteArrayOutputStream.toString());
                tableauJson = objetJson.getJSONArray("chapitre");

                for (int i = 0; i < tableauJson.length(); i++) {
                    position = tableauJson.getJSONObject(i).getInt("pos");
                    titre = tableauJson.getJSONObject(i).getString("title");
                    bouton=new Button(this);
                    bouton.setText( titre);
                    bouton.setWidth(400);
                    bouton.setHeight(200);
                    bouton.setTag(position);
                    bouton.setOnClickListener(listenerScrollBar);
                    layoutChapitre.addView(bouton);
                }
            } catch (JSONException e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /********************************************************************
     * charge le fichier json contenant les données des points de la map *
     * création des point sur la map                                     *
     * ajout listener aux point de la carte                              *
     * @param une map google                                             *
     * @return une map google                                            *
     *****************************************************************/

    public GoogleMap creationWayPointMapGoogle(GoogleMap googleMap) {
        JSONObject objetJson;
        JSONArray tableauJson;
        long lat;
        long lng;
        String label;
        int timestamp;
        InputStream inputStream=getResources().openRawResource(R.raw.timestamp);
        ByteArrayOutputStream byteArrayOutputStream= new  ByteArrayOutputStream();
        try {

            int elem = inputStream.read();
            // lecture du fichier et stockage dans un InputStream
            while (elem != -1) {
                byteArrayOutputStream.write(elem);
                elem = inputStream.read();
            }
            inputStream.close();
            try {
                // recuperation des données et création des points surla map
                objetJson = new JSONObject(byteArrayOutputStream.toString());
                tableauJson = objetJson.getJSONArray( "Waypoints");
                for (int i = 0; i < tableauJson.length(); i++) {
                    lat= tableauJson.getJSONObject(i).getLong("lat");
                    label = tableauJson.getJSONObject(i).getString("label");
                    lng = tableauJson.getJSONObject(i).getLong("lng");
                    timestamp=tableauJson.getJSONObject(i).getInt( "timestamp");
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat,lng))
                            .title(label));
                    marker.setTag(timestamp);
                }
            } catch (JSONException e){
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        googleMap.setOnMarkerClickListener(ListenerWayPointGoogleMap);
        return googleMap;
    }
    /**************************************************************
     * charge la map e les points                                  *
    ***************************************************************/
    public void chargeMap(){
        mMapView.getMapAsync(new OnMapReadyCallback(){
            public void onMapReady(GoogleMap googleMap){
                googleMap=creationWayPointMapGoogle(googleMap);
            }
        });

    }
    /*****************************************************
     * recharge la video à la position ou on s'etait arrété *
    *******************************************************/
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        videoRoute66.seekTo(ancienPosition);
        videoRoute66.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        ancienPosition= videoRoute66.getCurrentPosition();
        videoRoute66.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       mMapView.onDestroy();

    }
    protected void onStop(){
        super.onStop();
        mMapView.onStop();
    }
    protected void onStart(){
        super.onStart();
        mMapView.onStart();
    }
    /****************************************************************
     * recharge la position de la vidéo                              *
     * ***************************************************************/
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        ancienPosition = savedInstanceState.getInt("duree");
        Log.i("loadPosition", Integer.toString(ancienPosition));
    }

}
