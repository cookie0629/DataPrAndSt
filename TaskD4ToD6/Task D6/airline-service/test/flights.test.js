const supertest = require('supertest');
const app = require('../app.js');
const request = supertest(app);

// PG0247 flight_id=11057: On Time, 24 Economy seats available (2025-12-01)
// PG0009 flight_id=11063: On Time, 46 Economy seats available (2025-12-01)

describe('Testing endpoints for flights resource', () => {
  it('POST "/checkins" should return for correct request created boarding pass with seat number and correct status', async () => {
    const booking = {
      flightNo: 'PG0247',
      flightDate: '2025-12-01',
      fareConditions: 'Economy',
      passengerId: '_checkin_01',
      passengerName: 'Checkin Test',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(201);
      const { ticketNo } = response.body;
      return request.post('/checkins').send({ ticketNo }).then((res) => {
        expect(res.status).toBe(201);
        expect(res.headers['content-type']).toMatch('application/json');
        expect(res.body).toHaveProperty('boardingNo');
        expect(res.body).toHaveProperty('seatNo');
      });
    });
  });

  it('POST "/checkins" should return BadRequest error with message when trying to re-check in for a flight', async () => {
    const booking = {
      flightNo: 'PG0009',
      flightDate: '2025-12-01',
      fareConditions: 'Economy',
      passengerId: '_checkin_02',
      passengerName: 'Recheckin Test',
    };
    return request.post('/bookings').send(booking).then((response) => {
      expect(response.status).toBe(201);
      const { ticketNo } = response.body;
      return request.post('/checkins').send({ ticketNo }).then((res1) => {
        expect(res1.status).toBe(201);
        return request.post('/checkins').send({ ticketNo }).then((res2) => {
          expect(res2.status).toBe(400);
          expect(res2.body.message).toBeDefined();
        });
      });
    });
  });

  it('POST "/checkins" should return for non-existing ticket NotFound error with message', async () => {
    return request.post('/checkins').send({ ticketNo: 'nonexistent00' }).then((res) => {
      expect(res.status).toBe(404);
      expect(res.body.message).toBeDefined();
    });
  });

  it('POST "/checkins" should return for incorrect request body BadRequest error with message', async () => {
    return request.post('/checkins').send({}).then((response) => {
      expect(response.status).toBe(400);
      expect(response.body.message).toBeDefined();
    });
  });
});
