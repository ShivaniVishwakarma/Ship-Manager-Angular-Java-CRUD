package com.hpc.shipservice.service;

import com.hpc.shipservice.entity.Ship;
import com.hpc.shipservice.entity.User;
import com.hpc.shipservice.models.JwtRequest;
import com.hpc.shipservice.models.Response;
import com.hpc.shipservice.repository.ShipRepository;
import com.hpc.shipservice.repository.UserRepository;
import com.hpc.shipservice.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ShipService {

    @Autowired
    ShipCodeGenerator shipCodeGenerator;

    @Autowired
    ShipRepository shipRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    JwtUserDetailsService userDetailsService;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    public ResponseEntity<Response> addNewShipInfo(Ship ship){
        Response response = new Response();
        //Optional<Ship> s = shipRepository.findByShipCode(ship.getShipCode());
            Ship newShip = shipRepository.save(ship);
            String code = shipCodeGenerator.generateShipCode(newShip.getId());
            Optional<Ship> s1 = shipRepository.findById(newShip.getId());
            newShip.setShipCode(code);
            if(s1.isPresent()) {
                shipRepository.save(newShip);
            }
            response.setMessage("Ship Added Successfully");
            response.setStatus(true);
            response.setData(newShip);

        return ResponseEntity.ok(response);
    }

    public Collection<Ship> getShips() {
        return shipRepository.findAll();
    }

    public Optional<Ship> getShipsByShipCode(String shipCode) {
        return shipRepository.findByShipCode(shipCode);
    }

    public ResponseEntity<?> updateShipInfo(Ship ship) {
        Response response = new Response();
        Optional<Ship> s = shipRepository.findByShipCode(ship.getShipCode());
        if (s.isPresent()) {
            shipRepository.save(ship);
            response.setMessage("Ship Updated Successfully");
            response.setStatus(true);
        } else {
            response.setMessage("Ship does not exists with " + ship.getShipCode());
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> deleteShipInfo(String shipCode) {
        Response response = new Response();
        Optional<Ship> s = shipRepository.findByShipCode(shipCode);

        if (!s.isPresent()) {
            response.setMessage("Ship does not exist");
        } else {
            shipRepository.deleteById(shipCode);
            response.setMessage("Ship deleted successfully");
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> authenticate(JwtRequest u) {
        Response response = new Response();
        String jwt = null;
        System.out.println(bCryptPasswordEncoder.encode(u.getPassword()));
        Optional<User> user = userRepository.getUserByUsername(u.getUsername(), bCryptPasswordEncoder.encode(u.getPassword()));
        if(!user.isPresent()){
            response.setMessage("Invalid username or password");
        }else{
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(u.getUsername(), u.getPassword()));
            }catch(BadCredentialsException e){
                e.printStackTrace();
                response.setMessage("Invalid password");
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(u.getUsername());
            System.out.println(userDetails);
            jwt = jwtTokenUtil.generateToken(userDetails);
            String temp ="$2a$10$2SKDbWdrk3TLV0LiS5KJ2uHvCadmjvChu8FN2EVtloK3yob9mXfxq";
            response.setData(temp);
            response.setStatus(true);
            //response.setData(jwt);
            response.setMessage("Authentication success");
            System.out.println(jwt);
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<List<Ship>> getAllSortedShips(String[] sort) {
        try {
            List<Sort.Order> orders = new ArrayList<Sort.Order>();

            if (sort[0].contains(",")) {
                // will sort more than 2 fields
                // sortOrder="field, direction"
                //localhost:8080/ships/getAllSortedShips?sort=id,desc&sort=shipCode,desc
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
                }
            } else {
                //localhost:8080/ships/getAllSortedShips?sort=id,desc
                // sort=[field, direction]
                orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
            }

            List<Ship> Ships = shipRepository.findAll(Sort.by(orders));

            if (Ships.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(Ships, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equals("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equals("desc")) {
            return Sort.Direction.DESC;
        }

        return Sort.Direction.ASC;
    }

    public ResponseEntity<List<Ship>> getAllShipsPage(String shipName, int page, int size, String[] sort) {

        try {
            List<Sort.Order> orders = new ArrayList<Sort.Order>();

            if (sort[0].contains(",")) {
                // will sort more than 2 fields
                // sortOrder="field, direction"
                for (String sortOrder : sort) {
                    String[] _sort = sortOrder.split(",");
                    orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
                }
            } else {
                // sort=[field, direction]
                orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
            }

            List<Ship> Ships = new ArrayList<Ship>();
            Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

            Page<Ship> pageTuts;
            if (shipName == null)
                pageTuts = shipRepository.findAll(pagingSort);
            else
                pageTuts = shipRepository.findByShipNameContaining(shipName, pagingSort);

            Ships = pageTuts.getContent();

            Map<String, Object> response = new HashMap<>();
            response.put("Ships", Ships);
            response.put("currentPage", pageTuts.getNumber());
            response.put("totalItems", pageTuts.getTotalElements());
            response.put("totalPages", pageTuts.getTotalPages());

            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }
}

