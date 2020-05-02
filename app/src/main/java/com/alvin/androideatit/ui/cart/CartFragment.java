package com.alvin.androideatit.ui.cart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alvin.androideatit.Adapter.MyCartAdapter;
import com.alvin.androideatit.Callback.ILoadTimeFromFirebaseListener;
import com.alvin.androideatit.Common.Common;
import com.alvin.androideatit.Common.MySwipeHelper;
import com.alvin.androideatit.Database.CartDataSource;
import com.alvin.androideatit.Database.CartDatabase;
import com.alvin.androideatit.Database.CartItem;
import com.alvin.androideatit.Database.LocalCartDataSource;
import com.alvin.androideatit.EventBus.CounterCartEvent;
import com.alvin.androideatit.EventBus.HideFABCart;
import com.alvin.androideatit.EventBus.UpdateItemInCart;
import com.alvin.androideatit.Model.Order;
import com.alvin.androideatit.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener {

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    LocationManager locationManager;

    ILoadTimeFromFirebaseListener listener;

    @BindView(R.id.txt_text)
    CardView txt_text;
    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @OnClick(R.id.btn_place_holder)
    void onPlaceHolderClick(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more Step!");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_holder, null);

        EditText edt_address = view.findViewById(R.id.edt_address);
        EditText edt_comment = view.findViewById(R.id.edt_comment);
        TextView txt_address_detail = view.findViewById(R.id.txt_address_detail);
        RadioButton rdi_home = view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship = view.findViewById(R.id.rdi_ship_address);
        RadioButton rdi_cod = view.findViewById(R.id.rdi_cod);
//        RadioButton rdi_braintree = view.findViewById(R.id.rdi_braintree);


        // Set data
        edt_address.setText(Common.currentUser.getAddress()); // Default from user(rdi_home)
        edt_address.setEnabled(false);

        // Event
        rdi_home.setOnCheckedChangeListener((buttonView, b) -> {
            if (b)
            {
                edt_address.setText(Common.currentUser.getAddress());
                edt_address.setEnabled(false);
                txt_address_detail.setVisibility(View.GONE);
            }
        });
        rdi_other_address.setOnCheckedChangeListener((buttonView, b) -> {
            if (b)
            {
                edt_address.setText("");
                edt_address.setEnabled(true);
                txt_address_detail.setVisibility(View.GONE);
            }
        });
        rdi_ship.setOnCheckedChangeListener((buttonView, b) -> {

            if (b)
            {
                if (isLocationEnabled(buttonView)){
                    fusedLocationProviderClient.getLastLocation()
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                txt_address_detail.setVisibility(View.GONE);
                            })
                            .addOnCompleteListener(task -> {

                                String coordinates = new StringBuilder()
                                        .append(task.getResult().getLatitude())
                                        .append("/")
                                        .append(task.getResult().getLongitude()).toString();


                                Single<String> singleAddress = Single.just(getAddressFromLating(task.getResult().getLatitude()
                                        , task.getResult().getLongitude()));

                                Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>(){

                                    @Override
                                    public void onSuccess(String s) {
                                        edt_address.setText(s);
                                        edt_address.setEnabled(false);
                                        txt_address_detail.setText(coordinates);
                                        txt_address_detail.setVisibility(View.VISIBLE);

                                        rdi_ship.setChecked(true); // checked if success

                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                    //edt_address.setText(coordinates);
                                    //edt_address.setEnabled(false);
                                        edt_address.setVisibility(View.VISIBLE);
                                        txt_address_detail.setText(e.getMessage());
                                        txt_address_detail.setVisibility(View.VISIBLE);
                                    }
                                });

                            });
                }else {
                    edt_address.setEnabled(false);
                } // end of Check Location
            }
        });


        builder.setView(view)
                .setNegativeButton("NO", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .setPositiveButton("YES", (dialogInterface, which) -> {
                    //Toast.makeText(getContext(), "Implement late!", Toast.LENGTH_SHORT).show();
                    if (rdi_cod.isChecked())
                        paymentCOD(edt_address.getText().toString(), edt_comment.getText().toString());
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        Button bg1, bg2;
        bg1 = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        bg2 = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        bg1.setBackgroundColor(Color.TRANSPARENT);
        bg1.setTextColor(getResources().getColor(R.color.alert_button_color));
        bg2.setBackgroundColor(Color.TRANSPARENT);
        bg2.setTextColor(getResources().getColor(R.color.alert_button_color));

    }

    private boolean isLocationEnabled(CompoundButton buttonView){

        locationManager = null;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        if ( locationManager == null ) {
            locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        }
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex){}
        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex){}
        if ( !gps_enabled && !network_enabled ){
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setMessage("Location not enabled. To continue, Please turn on device location")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog1, which) -> {
                    //this will navigate user to the device location settings screen
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                    dialog1.dismiss();
                    })
                    .setNegativeButton("NO THANKS", (dialog1, which) -> {
                        dialog1.dismiss();
                        buttonView.setChecked(false);
                    });

            AlertDialog alert = dialog.create();
            alert.show();

            Button bg1, bg2;
            bg1 = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            bg2 = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            bg1.setBackgroundColor(Color.TRANSPARENT);
            bg1.setTextColor(getResources().getColor(R.color.alert_button_color));
            bg2.setBackgroundColor(Color.TRANSPARENT);
            bg2.setTextColor(getResources().getColor(R.color.alert_button_color));

        }

        return gps_enabled;
    }


    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cartItems -> {

            // When have all cartItems, get total price
            cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Double>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Double totalPrice) {

                            double finalPrice = totalPrice;

                            Order order = new Order();
                            order.setUserId(Common.currentUser.getUid());
                            order.setUserName(Common.currentUser.getName());
                            order.setUserPhone(Common.currentUser.getPhone());
                            order.setShippingAddress(address);

                            if (comment.length() == 0)
                                order.setComment("-");
                            else
                                order.setComment(comment);

                            if (currentLocation != null)
                            {
                                order.setLat(currentLocation.getLatitude());
                                order.setLng(currentLocation.getLongitude());
                            }
                            else
                            {
                                order.setLat(-0.1f);
                                order.setLng(-0.1f);
                            }

                            order.setCartItemList(cartItems);
                            order.setTotalPayment(totalPrice);
                            order.setDiscount(0);
                            order.setFinalPayment(finalPrice);
                            order.setCod(true);
                            order.setTransactionId("Cash On Delivery");

                            // Submit object to Firebase
                           // writeOrderToFirebase(order);
                            syncLocalTimeWithGlobaltime(order);
                        }

                        @Override
                        public void onError(Throwable e) {
//                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }, throwable -> {
            Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void syncLocalTimeWithGlobaltime(Order order) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMs = System.currentTimeMillis() + offset;

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                Date resultDate = new Date(estimatedServerTimeMs);

                Log.d("OrderDate", ""+sdf.format(resultDate));

                // Date to Long to Firebase
                listener.onLoadTimeSuccess(order, estimatedServerTimeMs);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
            }
        });
    }

    private void writeOrderToFirebase(Order order) {

        FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                //Create order Number with only digit
                .child(Common.createOrderNumber())
                .setValue(order)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    cartDataSource.cleanCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            
                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            // Clean after Success and update CounterFAB
                            EventBus.getDefault().postSticky(new CounterCartEvent(true)); // update FAB
                            Toast.makeText(getContext(), "Order placed Successfully!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Throwable e) {
//                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

    }

    private String getAddressFromLating(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result ="";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0)
            {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            }
            else
            {
                result ="Address NOT FOUND!";
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    private MyCartAdapter adapter;

    private Unbinder unbinder;

    private CartViewModel cartViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                ViewModelProviders.of(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        listener = this;

        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(this, cartItems -> {
            if (cartItems == null || cartItems.isEmpty())
            {
                recycler_cart.setVisibility(View.GONE);
                txt_text.setVisibility(View.GONE);
                group_place_holder.setVisibility(View.GONE);
                txt_empty_cart.setVisibility(View.VISIBLE);
            }
            else
            {
                recycler_cart.setVisibility(View.VISIBLE);
                txt_text.setVisibility(View.VISIBLE);
                group_place_holder.setVisibility(View.VISIBLE);
                txt_empty_cart.setVisibility(View.GONE);

                adapter = new MyCartAdapter(getContext(), cartItems);
                recycler_cart.setAdapter(adapter);
            }
        });

        unbinder = ButterKnife.bind(this, root);
        initViews();
        initLocation();
        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallback();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void initViews() {

        setHasOptionsMenu(true);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        EventBus.getDefault().postSticky(new HideFABCart(true));

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            sumAllItemInCart(); // Update total PRICE in CART
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true)); // update FAB
                                            Toast.makeText(getContext(), "Delete item from Cart successful!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }));
            }
        };
        
        sumAllItemInCart();
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(aDouble));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), "Cart is empty", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_refresh).setVisible(false); // Hide Home menu already inflate
        menu.findItem(R.id.action_search).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart)
        {
            cleanCart();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cleanCart(){
        cartDataSource.cleanCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        Toast.makeText(getContext(), "Clear Cart Success", Toast.LENGTH_SHORT).show();
                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {
        cartViewModel.onStop();

        EventBus.getDefault().postSticky(new HideFABCart(false));

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        compositeDisposable.clear();

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        EventBus.getDefault().postSticky(new HideFABCart(true));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event)
    {
        if (event.getCartItem() != null)
        {
            // First, save state of Recycler view
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            // Fix error Refresh view after update
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $")
                                .append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
//                        if (e.getMessage().contains("Query returned empty"))
//                            Toast.makeText(getContext(), "[SUM CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onLoadTimeSuccess(Order order, long estimateTimeInMs) {
        order.setCreateDate(estimateTimeInMs);
        order.setOrderStatus(0);

        // Create True Date & set OrderDate
        long estimatedServerTimeMs = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date resultDate = new Date(estimatedServerTimeMs);
        String finalResultDate = String.valueOf(resultDate);    // Create Long to Date to String value

        order.setOrderDate(finalResultDate);

        // Send to Firebase
        writeOrderToFirebase(order);
    }


    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), ""+message, Toast.LENGTH_SHORT).show();
    }
}