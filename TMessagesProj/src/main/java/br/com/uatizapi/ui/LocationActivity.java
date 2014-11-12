/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package br.com.uatizapi.ui;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import br.com.uatizapi.android.AndroidUtilities;
import br.com.uatizapi.android.ContactsController;
import br.com.uatizapi.messenger.FileLog;
import br.com.uatizapi.android.LocaleController;
import br.com.uatizapi.messenger.TLRPC;
import br.com.uatizapi.android.MessageObject;
import br.com.uatizapi.android.MessagesController;
import br.com.uatizapi.android.NotificationCenter;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarLayer;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenu;
import br.com.uatizapi.ui.Views.ActionBar.ActionBarMenuItem;
import br.com.uatizapi.ui.Views.BackupImageView;
import br.com.uatizapi.ui.Views.ActionBar.BaseFragment;

import java.util.List;

public class LocationActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private GoogleMap googleMap;
    private TextView distanceTextView;
    private Marker userMarker;
    private Location myLocation;
    private Location userLocation;
    private MessageObject messageObject;
    private BackupImageView avatarImageView;
    private TextView nameTextView;
    private boolean userLocationMoved = false;
    private boolean firstWas = false;
    private MapView mapView;
    private LocationActivityDelegate delegate;

    private final static int map_to_my_location = 1;
    private final static int map_list_menu_map = 2;
    private final static int map_list_menu_satellite = 3;
    private final static int map_list_menu_hybrid = 4;

    public static interface LocationActivityDelegate {
        public abstract void didSelectLocation(double latitude, double longitude);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        swipeBackEnabled = false;
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        if (messageObject != null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, br.com.uatizapi.messenger.R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(br.com.uatizapi.messenger.R.layout.updating_state_layout);
            if (messageObject != null) {
                actionBarLayer.setTitle(LocaleController.getString("ChatLocation", br.com.uatizapi.messenger.R.string.ChatLocation));
            } else {
                actionBarLayer.setTitle(LocaleController.getString("ShareLocation", br.com.uatizapi.messenger.R.string.ShareLocation));
            }

            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == map_list_menu_map) {
                        if (googleMap != null) {
                            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                    } else if (id == map_list_menu_satellite) {
                        if (googleMap != null) {
                            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        }
                    } else if (id == map_list_menu_hybrid) {
                        if (googleMap != null) {
                            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        }
                    } else if (id == map_to_my_location) {
                        if (myLocation != null) {
                            LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                            if (googleMap != null) {
                                CameraUpdate position = CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 8);
                                googleMap.animateCamera(position);
                            }
                        }
                    }
                }
            });

            ActionBarMenu menu = actionBarLayer.createMenu();
            menu.addItem(map_to_my_location, br.com.uatizapi.messenger.R.drawable.ic_ab_location);

            ActionBarMenuItem item = menu.addItem(0, br.com.uatizapi.messenger.R.drawable.ic_ab_other);
            item.addSubItem(map_list_menu_map, LocaleController.getString("Map", br.com.uatizapi.messenger.R.string.Map), 0);
            item.addSubItem(map_list_menu_satellite, LocaleController.getString("Satellite", br.com.uatizapi.messenger.R.string.Satellite), 0);
            item.addSubItem(map_list_menu_hybrid, LocaleController.getString("Hybrid", br.com.uatizapi.messenger.R.string.Hybrid), 0);

            if (messageObject != null) {
                fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.location_view_layout, container, false);
            } else {
                fragmentView = inflater.inflate(br.com.uatizapi.messenger.R.layout.location_attach_layout, container, false);
            }

            avatarImageView = (BackupImageView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.location_avatar_view);
            if (avatarImageView != null) {
                avatarImageView.processDetach = false;
            }
            nameTextView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.location_name_label);
            distanceTextView = (TextView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.location_distance_label);
            View bottomView = fragmentView.findViewById(br.com.uatizapi.messenger.R.id.location_bottom_view);
            TextView sendButton = (TextView) fragmentView.findViewById(br.com.uatizapi.messenger.R.id.location_send_button);
            if (sendButton != null) {
                sendButton.setText(LocaleController.getString("SendLocation", br.com.uatizapi.messenger.R.string.SendLocation));
            }

            mapView = (MapView)fragmentView.findViewById(br.com.uatizapi.messenger.R.id.map_view);
            mapView.onCreate(null);
            try {
                MapsInitializer.initialize(getParentActivity());
                googleMap = mapView.getMap();
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }

            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        positionMarker(location);
                    }
                });
                myLocation = getLastLocation();

                if (sendButton != null) {
                    userLocation = new Location("network");
                    userLocation.setLatitude(20.659322);
                    userLocation.setLongitude(-11.406250);
                    LatLng latLng = new LatLng(20.659322, -11.406250);
                    userMarker = googleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(br.com.uatizapi.messenger.R.drawable.map_pin)).draggable(true));

                    sendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (delegate != null) {
                                delegate.didSelectLocation(userLocation.getLatitude(), userLocation.getLongitude());
                            }
                            finishFragment();
                        }
                    });

                    googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                        @Override
                        public void onMarkerDragStart(Marker marker) {
                        }

                        @Override
                        public void onMarkerDrag(Marker marker) {
                            userLocationMoved = true;
                        }

                        @Override
                        public void onMarkerDragEnd(Marker marker) {
                            LatLng latLng = marker.getPosition();
                            userLocation.setLatitude(latLng.latitude);
                            userLocation.setLongitude(latLng.longitude);
                        }
                    });
                }

                if (bottomView != null) {
                    bottomView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (userLocation != null) {
                                LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                                CameraUpdate position = CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 8);
                                googleMap.animateCamera(position);
                            }
                        }
                    });
                }

                if (messageObject != null) {
                    int fromId = messageObject.messageOwner.from_id;
                    if (messageObject.messageOwner instanceof TLRPC.TL_messageForwarded) {
                        fromId = messageObject.messageOwner.fwd_from_id;
                    }
                    TLRPC.User user = MessagesController.getInstance().getUser(fromId);
                    if (user != null) {
                        TLRPC.FileLocation photo = null;
                        if (user.photo != null) {
                            photo = user.photo.photo_small;
                        }
                        avatarImageView.setImage(photo, "50_50", AndroidUtilities.getUserAvatarForId(user.id));
                        nameTextView.setText(ContactsController.formatName(user.first_name, user.last_name));
                    }
                    userLocation = new Location("network");
                    userLocation.setLatitude(messageObject.messageOwner.media.geo.lat);
                    userLocation.setLongitude(messageObject.messageOwner.media.geo._long);
                    LatLng latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                    userMarker = googleMap.addMarker(new MarkerOptions().position(latLng).
                            icon(BitmapDescriptorFactory.fromResource(br.com.uatizapi.messenger.R.drawable.map_pin)));
                    CameraUpdate position = CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 8);
                    googleMap.moveCamera(position);
                }

                positionMarker(myLocation);
            }
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    private Location getLastLocation() {
        LocationManager lm = (LocationManager) ApplicationLoader.applicationContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location l = null;
        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) {
                break;
            }
        }
        return l;
    }

    private void updateUserData() {
        if (messageObject != null && avatarImageView != null) {
            int fromId = messageObject.messageOwner.from_id;
            if (messageObject.messageOwner instanceof TLRPC.TL_messageForwarded) {
                fromId = messageObject.messageOwner.fwd_from_id;
            }
            TLRPC.User user = MessagesController.getInstance().getUser(fromId);
            if (user != null) {
                TLRPC.FileLocation photo = null;
                if (user.photo != null) {
                    photo = user.photo.photo_small;
                }
                avatarImageView.setImage(photo, null, AndroidUtilities.getUserAvatarForId(user.id));
                nameTextView.setText(ContactsController.formatName(user.first_name, user.last_name));
            }
        }
    }

    private void positionMarker(Location location) {
        if (location == null) {
            return;
        }
        myLocation = location;
        if (messageObject != null) {
            if (userLocation != null && distanceTextView != null) {
                float distance = location.distanceTo(userLocation);
                if (distance < 1000) {
                    distanceTextView.setText(String.format("%d %s", (int)(distance), LocaleController.getString("MetersAway", br.com.uatizapi.messenger.R.string.MetersAway)));
                } else {
                    distanceTextView.setText(String.format("%.2f %s", distance / 1000.0f, LocaleController.getString("KMetersAway", br.com.uatizapi.messenger.R.string.KMetersAway)));
                }
            }
        } else {
            if (!userLocationMoved && googleMap != null) {
                userLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                userMarker.setPosition(latLng);
                if (firstWas) {
                    CameraUpdate position = CameraUpdateFactory.newLatLng(latLng);
                    googleMap.animateCamera(position);
                } else {
                    firstWas = true;
                    CameraUpdate position = CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 8);
                    googleMap.moveCamera(position);
                }
            }
        }
    }

    public void setMessageObject(MessageObject message) {
        messageObject = message;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer)args[0];
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0 || (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                updateUserData();
            }
        } else if (id == NotificationCenter.closeChats) {
            removeSelfFromStack();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            try {
                mapView.onPause();
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    public void setDelegate(LocationActivityDelegate delegate) {
        this.delegate = delegate;
    }
}
