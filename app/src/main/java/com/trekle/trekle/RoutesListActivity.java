package com.trekle.trekle;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.trekle.trekle.models.Route;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RoutesListActivity extends AppCompatActivity {

    private String TAG = "ROUTELIST";

    public class RouteViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private View view;
        private String pathDocument;

        RouteViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            view.setOnClickListener(v -> {
                Intent intent = new Intent(RoutesListActivity.this, MainActivity.class);
                int position = getAdapterPosition();
                Route route = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable("route", route);
                intent.putExtras(bundle);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
            itemView.setOnCreateContextMenuListener(this);

        }

        void setName(String name) {
            TextView textView = view.findViewById(R.id.route_name_value);
            textView.setText(name);
        }

        void setAscent(Double value) {
            TextView textView = view.findViewById(R.id.route_ascent_value);
            textView.setText(value.toString());
        }

        void setDistance(Double value) {
            TextView textView = view.findViewById(R.id.route_distance_value);
            textView.setText(value.toString());
        }

        void setPathDocument(String path) {
            this.pathDocument = path;
        }

        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            MenuItem edit = contextMenu.add(Menu.NONE, 1, 1, "Edit");
            MenuItem delete = contextMenu.add(Menu.NONE, 2, 2, "Delete");
            edit.setOnMenuItemClickListener(onEditMenu);
            delete.setOnMenuItemClickListener(onEditMenu);
        }

        private final MenuItem.OnMenuItemClickListener onEditMenu = item -> {

            switch (item.getItemId()) {
                case 1:
                    Toast.makeText(getApplicationContext(), "edit", Toast.LENGTH_LONG).show();
                    break;

                case 2:
                    Toast.makeText(getApplicationContext(), "delete "+ pathDocument, Toast.LENGTH_LONG).show();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    View rootView = RoutesListActivity.this.findViewById(R.id.routes_list_view);
                    new AlertDialog.Builder(RoutesListActivity.this)
                            .setTitle(R.string.title_dialog_delete_route)
                            .setMessage(R.string.text_dialog_delete_route)
                            .setPositiveButton("Yes", (dialog, which) -> {
                                db.document(pathDocument)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Snackbar.make(rootView, R.string.success_delete_route, Snackbar.LENGTH_LONG).show())
                                        .addOnFailureListener(e -> Snackbar.make(rootView, R.string.error_delete_route, Snackbar.LENGTH_LONG).show());
                            })
                            .setNegativeButton("no", null)
                            .show();
                    break;
            }
            return true;
        };
    }
    private FirestoreRecyclerAdapter<Route, RouteViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes_list);

        RecyclerView recyclerView = findViewById(R.id.routes_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FirebaseFirestore rootRef = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        Query query = rootRef.collection("users").document(user.getUid()).collection("routes");
        FirestoreRecyclerOptions<Route> options = new FirestoreRecyclerOptions.Builder<Route>()
                .setQuery(query, Route.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Route, RouteViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RouteViewHolder holder, int position, @NonNull Route route) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) getSnapshots().getSnapshot(position);
                String key = doc.getId();
                holder.setName(route.getName());
                holder.setDistance(route.getDistance());
                holder.setAscent(route.getAscent());
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                holder.setPathDocument("/users/"+user.getUid()+"/routes/"+key);
            }

            @NonNull
            @Override
            public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
                return new RouteViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
